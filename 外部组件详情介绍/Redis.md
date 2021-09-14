

# 动态字符串

在版本2.0的时候还可以在sds.h的文件中看见关于sds的结构体说明

```c
//  字符串本身就是是一个字符数组
struct sdshdr {
    int len;
    int free;
    char buf[];//这是柔性数组，可不断地向尾部添加数据，通过malloc函数
为柔性数组动态分配内存
};
```

但2.0以后就看不到上面那么明显的定义

主要在于考虑到资源的进一步优化，因为上面的定义是，无论字符串多大，都需要一个4字节的变量来计算长度，显然是浪费。

因此，下面的几种定义是应对不同字符串长度是对应的结构体，主要是长度变量从1字节到8字节，

```c
/*
结构体会按其所有变量
大小的最小公倍数做字节对齐，而用packed修饰后，结构体则变为按1字节对齐。以sdshdr32
为例，修饰前按4字节对齐大小为12(4×3)字节；修饰后按1字节对齐，注意buf是个char类型的
柔性数组，地址连续，始终在flags之后。
*/

/* Note: sdshdr5 is never used, we just access the flags byte directly.
 * However is here to document the layout of type 5 SDS strings. */
struct __attribute__ ((__packed__)) sdshdr5 {
    // 标识当前结构体的类型，低3位用作标识位，高5位预留。
    // 使用了1字节计算长度，但实际只有高5位参与计算，就是最多承担长度2^5
    unsigned char flags; /* 3 lsb of type, and 5 msb of string length */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr8 {
    uint8_t len; /* used */
    
    // 表示buf中已分配字节数，不同于free，记录的是为buf分配的总长度
    uint8_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */ 仅用3位表示结构体类型
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr16 {
    uint16_t len; /* used */
    uint16_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr32 {
    uint32_t len; /* used */
    uint32_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
struct __attribute__ ((__packed__)) sdshdr64 {
    uint64_t len; /* used */
    uint64_t alloc; /* excluding the header and null terminator */
    unsigned char flags; /* 3 lsb of type, 5 unused bits */
    char buf[];
};
// buf[-1] 是flags
// 因为此时按1字节对齐，通过flags与7与运算获取对应的类型，然后获取该类型对应的字节长度
// 那么我们经历一次指定的字节长度，就获取了一次对应的数据
```

## 创建

```c
// 版本5 的时候还没有 int trymalloc 参数
// initlen就是实际需要的数据的字节长度
sds _sdsnewlen(const void *init, size_t initlen, int trymalloc) {
    void *sh; sds s;
    char type = sdsReqType(initlen);
    /* Empty strings are usually created in order to append. Use type 8
     * since type 5 is not good at this. */
    // 这里为了避免以后的扩容，干脆就使用了 SDS_TYPE_8 类型
    if (type == SDS_TYPE_5 && initlen == 0) type = SDS_TYPE_8;
    // 上面的type获取对应 字符串符合的结构体类型
    // 下面就是利用对应的结构体类型获取对应的结构体的头部需要的字节数
    int hdrlen = sdsHdrSize(type); 
    unsigned char *fp; /* flags pointer. */
    size_t usable;

    assert(initlen + hdrlen + 1 > initlen); /* Catch size_t overflow */
    // 获取对应大小的一块内存，头部空间++字符组+最后的"\0"
    sh = trymalloc ? s_trymalloc_usable(hdrlen+initlen+1, &usable) : 					                                         s_malloc_usable(hdrlen+initlen+1, &usable);
    if (sh == NULL) return NULL;
    if (init==SDS_NOINIT) init = NULL;
    else if (!init) memset(sh, 0, hdrlen+initlen+1);
    s = (char*)sh+hdrlen; // 这里就是获取字符数组的头指针
    fp = ((unsigned char*)s)-1; // 相当于buf[-1]
    usable = usable-hdrlen-1;
    if (usable > sdsTypeMaxSize(type))
        usable = sdsTypeMaxSize(type);
    switch(type) {
        case SDS_TYPE_5: {
            *fp = type | (initlen << SDS_TYPE_BITS);break;
        }
        case SDS_TYPE_8: {
            SDS_HDR_VAR(8,s);sh->len = initlen;sh->alloc = usable;*fp = type;break;
        }
        case SDS_TYPE_16: {
            SDS_HDR_VAR(16,s);sh->len = initlen;sh->alloc = usable;*fp = type;break;
        }
        case SDS_TYPE_32: {
            SDS_HDR_VAR(32,s);sh->len = initlen;sh->alloc = usable;*fp = type;break;
        }
        case SDS_TYPE_64: {
            SDS_HDR_VAR(64,s);sh->len = initlen; sh->alloc = usable;*fp = type;break;
        }
    }
    if (initlen && init) memcpy(s, init, initlen);
    s[initlen] = '\0';
    return s;
}
```

## 释放

```c
// 这种是实在的把内存空间给清空
void sdsfree(sds s) {
    if (s == NULL) return;
    s_free((char*)s-sdsHdrSize(s[-1]));
}

// 这种是抖机灵，就是把字符数组对应的长度主动地改成0，顺便把字符数组的第一个字符改成'\0'
// 这种方式没有清空内存，以后如果有新数据，可以直接覆盖上去
void sdsclear(sds s) {
    sdssetlen(s, 0);
    s[0] = '\0';
}
```

## 拼接

```c
sds sdscatsds(sds s, const sds t) {
    return sdscatlen(s, t, sdslen(t));
}
sds sdscatlen(sds s, const void *t, size_t len) {
    size_t curlen = sdslen(s);
	
    // 下面的函数涉及扩容操作
    // 考虑到扩容后，类型可能的变化，因此还需要再最后多做判断
    // 如果原本剩余的空间足够，则无需考虑太多
    // 如果不够，则需要整体需要的字节数是slen,大于1M，则扩容为slen+1M 空间
    // 否则，就直接扩容为 两倍 slen 空间
    s = sdsMakeRoomFor(s,len);
    if (s == NULL) return NULL;
    memcpy(s+curlen, t, len);
    sdssetlen(s, curlen+len);
    s[curlen+len] = '\0';
    return s;
}
```



# 跳表

 ## 结构

```c
// 跳表的节点
typedef struct zskiplistNode {
    sds ele;// 存储字符串类型
    double score; // 用于排序的分值
    struct zskiplistNode *backward; // 后退指针，只能指向当前节点最底层的前一个节点
    // 头节点和第一个节点的backward指向NULL，从后向前遍历跳跃表时使用
    
    struct zskiplistLevel {
        struct zskiplistNode *forward;
        unsigned long span;
    } level[]; // 柔性数组 
 // 每个节点的数组长度不一样，在生成跳跃表节点时，随机生成一个1～64的值，值越大出现的概率越低。  	 // level 数组中每项的元素为 
		// forward：指向本层下一个节点，尾节点的forward指向NULL 
		// span：forward指向的节点与本节点之间的元素个数。span值越大，跳过的节点个数越多 
} zskiplistNode;
/*************************************************/
typedef struct zskiplist {
    /* 头节点是跳跃表的一个特殊节点，它的level数组元素个
	数为64。头节点在有序集合中不存储任何member和score值，ele值为NULL，score值为0；也
	不计入跳跃表的总长度。头节点在初始化时，64个元素的forward都指向NULL，span值都为
	0。*/
    struct zskiplistNode *header, *tail;
    unsigned long length; // 表示除头节点之外的节点总数
    int level; // 跳跃表的高度
} zskiplist;
```

## 创建

> 节点层高的最小值为1，最大值是ZSKIPLIST_MAXLEVEL，Redis5中节点层高的值为
> 64。
>
> ```c
> #define ZSKIPLIST_MAXLEVEL 32 /* Should be enough for 2^64 elements */
> // 版本 5 之前是 64
> ```
>
> ```c
> #define ZSKIPLIST_P 0.25  // 作为概率
> 
> int zslRandomLevel(void) {
>     int level = 1;
>     /* 每次生成一个随机值，取这个值的低16
> 	   位作为x，当x小于0.25倍的0xFFFF时，level的值加1；否则退出while循环。最终返回level和
> 	   ZSKIPLIST_MAXLEVEL两者中的最小值*/
>     while ((random()&0xFFFF) < (ZSKIPLIST_P * 0xFFFF)) level += 1;
>     // 获取一个介于1~ZSKIPLIST_MAXLEVEL的节点高度
>     return (level<ZSKIPLIST_MAXLEVEL) ? level : ZSKIPLIST_MAXLEVEL;
> }
> ```
>
> 节点层高的期望值
> $$
> E&=&(1-p)\sum_{i=1}^{+\infty} ip^{i-1}\\
> &=& \frac{1}{1-p}
> $$
>
> ```c
> /* Create a new skiplist. */
> zskiplist *zslCreate(void) {
>     int j;
>     zskiplist *zsl;
> 
>     zsl = zmalloc(sizeof(*zsl));
>     zsl->level = 1;
>     zsl->length = 0;
>     /* 头节点是一个特殊的节点，不存储有序集合的member信息。头节点是跳跃表中第一个插
> 	   入的节点，其level数组的每项forward都为NULL，span值都为0。*/
>     zsl->header = zslCreateNode(ZSKIPLIST_MAXLEVEL,0,NULL);
>     for (j = 0; j < ZSKIPLIST_MAXLEVEL; j++) {
>         zsl->header->level[j].forward = NULL;
>         zsl->header->level[j].span = 0;
>     }
>     zsl->header->backward = NULL;
>     zsl->tail = NULL;
>     return zsl;
> }
> ```
>
> 

## 跳表节点

```c
zskiplistNode *zslCreateNode(int level, double score, sds ele) {
    // 同样的为节点申请一块内存
    zskiplistNode *zn = zmalloc(sizeof(*zn)+level*sizeof(struct zskiplistLevel));
    zn->score = score;
    zn->ele = ele;
    return zn;
}
```

## 插入

```c
// 插入节点的步骤：①查找要插入的位置；②调整跳跃表高度；③插入节点；④调整backward。
zskiplistNode *zslInsert(zskiplist *zsl, double score, sds ele) {
    zskiplistNode *update[ZSKIPLIST_MAXLEVEL], *x;
    unsigned int rank[ZSKIPLIST_MAXLEVEL];
    int i, level;

    serverAssert(!isnan(score));
    x = zsl->header;
    /*** 从层高的节点开始一个个查找，确定需要插入的位置 ***/
    for (i = zsl->level-1; i >= 0; i--) {
       	/* rank[]：记录当前层从header节点到update[i]节点所经历的步长，在更新update[i]的span		   	       和设置新插入节点的span时用到。*/
        rank[i] = i == (zsl->level-1) ? 0 : rank[i+1];
        while (x->level[i].forward && (x->level[i].forward->score < score ||
                                        (x->level[i].forward->score == score && 
                                        sdscmp(x->level[i].forward->ele,ele) < 0))){
            rank[i] += x->level[i].span;
            x = x->level[i].forward;
        }
        /* update[]：插入节点时，需要更新被插入节点每层的前一个节点。由于每层更新的节点不
		             一样，所以将每层需要更新的节点记录在update[i]中。*/
        update[i] = x;
    }
   
    /**** 调整高度 ******/
    level = zslRandomLevel();
    if (level > zsl->level) { // 主要应对新节点的高度超过跳表的高度
        for (i = zsl->level; i < level; i++) {
            rank[i] = 0;
            update[i] = zsl->header;
            update[i]->level[i].span = zsl->length;
        }
        zsl->level = level;
    }
    
    /****** 插入节点 *******/
    x = zslCreateNode(level,score,ele);
    for (i = 0; i < level; i++) { // 对新节点从0开始更新
        x->level[i].forward = update[i]->level[i].forward;
        update[i]->level[i].forward = x;

        /* update span covered by update[i] as x is inserted here */
        x->level[i].span = update[i]->level[i].span - (rank[0] - rank[i]);
        update[i]->level[i].span = (rank[0] - rank[i]) + 1;
    }

    /* increment span for untouched levels */
    for (i = level; i < zsl->level; i++) { update[i]->level[i].span++;}
	
    /***** 调整backward ***/
    x->backward = (update[0] == zsl->header) ? NULL : update[0];
    if (x->level[0].forward) x->level[0].forward->backward = x;
    else zsl->tail = x;
    zsl->length++;
    return x;
}
```

## 删除

```c
void zslDeleteNode(zskiplist *zsl, zskiplistNode *x, zskiplistNode **update) {
    int i;
    // 同样的，通过遍历跳表，获取对应节点所在各个层的信息
    for (i = 0; i < zsl->level; i++) {
        if (update[i]->level[i].forward == x) {
            update[i]->level[i].span += x->level[i].span - 1;
            update[i]->level[i].forward = x->level[i].forward;
        } else {
            update[i]->level[i].span -= 1;
        }
    }
    if (x->level[0].forward) {
        x->level[0].forward->backward = x->backward;
    } else {
        zsl->tail = x->backward;
    }
    while(zsl->level > 1 && zsl->header->level[zsl->level-1].forward == NULL)
        zsl->level--;
    zsl->length--;
}
```

```c
// 从跳表中删除具有匹配分数/元素的元素。
int zslDelete(zskiplist *zsl, double score, sds ele, zskiplistNode **node) {
    zskiplistNode *update[ZSKIPLIST_MAXLEVEL], *x;
    int i;

    x = zsl->header;
    for (i = zsl->level-1; i >= 0; i--) {
        while (x->level[i].forward && (x->level[i].forward->score < score ||
                                  (x->level[i].forward->score == score &&
                                  sdscmp(x->level[i].forward->ele,ele) < 0))){
            x = x->level[i].forward;
        }
        update[i] = x;
    }
    /* We may have multiple elements with the same score, what we need
     * is to find the element with both the right score and object. */
    x = x->level[0].forward;
    if (x && score == x->score && sdscmp(x->ele,ele) == 0) {
        zslDeleteNode(zsl, x, update);
        if (!node) zslFreeNode(x);
        else  *node = x;
        return 1;
    }
    return 0; /* not found */
}
```

```c
/* 清除整个跳表 */
void zslFree(zskiplist *zsl) {
    zskiplistNode *node = zsl->header->level[0].forward, *next;

    zfree(zsl->header);
    while(node) {
        next = node->level[0].forward;
        zslFreeNode(node);
        node = next;
    }
    zfree(zsl);
}
```

# 压缩列表

压缩列表ziplist本质上就是一个字节数组，是Redis为了节约内存而设计的一种线性数据结
构，可以包含多个元素，每个元素可以是一个字节数组或一个整数。

> Redis的有序集合、散列和列表都直接或者间接使用了压缩列表。当有序集合或散列表的
> 元素个数比较少，且元素都是短字符串时，Redis便使用压缩列表作为其底层数据存储结构。
> 列表使用快速链表（quicklist）数据结构存储，而快速链表就是双向链表与压缩列表的组合。

![](https://mudongjing.github.io/gallery/redis/zip/ziplist.png "Redis 5设计与源码分析 (数据库技术丛书) by 陈雷 等")

> - zlbytes： 压缩列表的字节长度，占4个字节，因此压缩列表最多有$2^{32}-1$个字节。
> - zltail： 压缩列表尾元素相对于压缩列表起始地址的偏移量，占4个字节。
> - zllen： 压缩列表的元素个数，占2个字节。zllen无法存储元素个数超过65535（$2^{16}-1$）的压缩列表，必须遍历整个压缩列表才能获取到元素个数。
> - entryX： 压缩列表存储的元素，可以是字节数组或者整数，长度不限。entry的编码结
>   构将在后面详细介绍。
> - zlend： 压缩列表的结尾，占1个字节，恒为0xFF。
>
> ```c
> /* Return total bytes a ziplist is composed of. */
> #define ZIPLIST_BYTES(zl)       (*((uint32_t*)(zl)))
> 
> /* Return the offset of the last item inside the ziplist. */
> #define ZIPLIST_TAIL_OFFSET(zl) (*((uint32_t*)((zl)+sizeof(uint32_t))))
> 
> /* Return the length of a ziplist, or UINT16_MAX if the length cannot be
>  * determined without scanning the whole ziplist. */
> #define ZIPLIST_LENGTH(zl)      (*((uint16_t*)((zl)+sizeof(uint32_t)*2)))
> 
> /* The size of a ziplist header: two 32 bit integers for the total
>  * bytes count and last item offset. One 16 bit integer for the number
>  * of items field. */
> #define ZIPLIST_HEADER_SIZE     (sizeof(uint32_t)*2+sizeof(uint16_t))
> 
> /* Size of the "end of ziplist" entry. Just one byte. */
> #define ZIPLIST_END_SIZE        (sizeof(uint8_t))
> 
> /* Return the pointer to the first entry of a ziplist. */
> #define ZIPLIST_ENTRY_HEAD(zl)  ((zl)+ZIPLIST_HEADER_SIZE)
> 
> /* Return the pointer to the last entry of a ziplist, using the
>  * last entry offset inside the ziplist header. */
> #define ZIPLIST_ENTRY_TAIL(zl)  ((zl)+intrev32ifbe(ZIPLIST_TAIL_OFFSET(zl)))
> 
> /* Return the pointer to the last byte of a ziplist, which is, the
>  * end of ziplist FF entry. */
> #define ZIPLIST_ENTRY_END(zl)   ((zl)+intrev32ifbe(ZIPLIST_BYTES(zl))-1)
> ```
>
> 压缩列表元素的结构
>
> ![](https://mudongjing.github.io/gallery/redis/zip/zipele.png "Redis 5设计与源码分析 (数据库技术丛书) by 陈雷 等")
>
> > - previous_entry_length字段表示前一个元素的字节长度，占1个或者5个字节，当前一个元
> >   素的长度小于254字节时，用1个字节表示；当前一个元素的长度大于或等于254字节时，用5
> >   个字节来表示。
> >   - 而此时previous_entry_length字段的第1个字节是固定的0xFE，后面4个字节才
> >     真正表示前一个元素的长度。
> >   - 假设已知当前元素的首地址为p，那么p-previous_entry_length就
> >     是前一个元素的首地址，从而实现压缩列表从尾到头的遍历。
> > - encoding字段表示当前元素的编码，即content字段存储的数据类型（整数或者字节数
> >   组），数据内容存储在content字段。为了节约内存，encoding字段同样长度可变。
>
> ![](https://mudongjing.github.io/gallery/redis/zip/zipcode.png)
>
> > - 根据encoding字段第1个字节的前2位，可以判断content字段存储的是整数或者
> >   字节数组（及其最大长度）。
> > - 当content存储的是字节数组时，后续字节标识字节数组的实际长度；
> > - 当content存储的是整数时，可根据第3、第4位判断整数的具体类型。
> > - 而当encoding字段标识当前元素存储的是0～12的立即数时，数据直接存储在encoding字段的最后4位，此时没有content字段。
> >
> > ```c
> > /* Different encoding/length possibilities */
> > #define ZIP_STR_MASK 0xc0
> > #define ZIP_INT_MASK 0x30
> > #define ZIP_STR_06B (0 << 6)
> > #define ZIP_STR_14B (1 << 6)
> > #define ZIP_STR_32B (2 << 6)
> > #define ZIP_INT_16B (0xc0 | 0<<4)
> > #define ZIP_INT_32B (0xc0 | 1<<4)
> > #define ZIP_INT_64B (0xc0 | 2<<4)
> > #define ZIP_INT_24B (0xc0 | 3<<4)
> > #define ZIP_INT_8B 0xfe
> > ```



## 元素结构

```c
typedef struct zlentry {
    unsigned int prevrawlensize; /* Bytes used to encode the previous entry len*/
    unsigned int prevrawlen;     /* Previous entry len. */
    unsigned int lensize;        /* Bytes used to encode this entry type/len.
                                    For example strings have a 1, 2 or 5 bytes
                                    header. Integers always use a single byte.*/
    unsigned int len;            /* Bytes used to represent the actual entry.
                                    For strings this is just the string length
                                    while for integers it is 1, 2, 3, 4, 8 or
                                    0 (for 4 bit immediate) depending on the
                                    number range. */
    unsigned int headersize;     /* prevrawlensize + lensize. */
    unsigned char encoding;      /* Set to ZIP_STR_* or ZIP_INT_* depending on
                                    the entry encoding. However for 4 bits
                                    immediate integers this can assume a range
                                    of values and must be range-checked. */
    unsigned char *p;            /* Pointer to the very start of the entry, that
                                    is, this points to prev-entry-len field. */
} zlentry;
```

## 创建

```c
// 创建空的压缩列表
unsigned char *ziplistNew(void) {
    unsigned int bytes = ZIPLIST_HEADER_SIZE + ZIPLIST_END_SIZE;
    /* 创建空的压缩列表，只需要分配初始存储空间11(4+4+2+1)个字节，并对zlbytes、zltail、zllen和zlend字		   段初始化即可。*/
    unsigned char *zl = zmalloc(bytes);
    ZIPLIST_BYTES(zl) = intrev32ifbe(bytes);
    ZIPLIST_TAIL_OFFSET(zl) = intrev32ifbe(ZIPLIST_HEADER_SIZE);
    ZIPLIST_LENGTH(zl) = 0;
    zl[bytes-1] = ZIP_END;
    return zl;
}
```

## 插入

```c
unsigned char *ziplistInsert(unsigned char *zl, unsigned char *p, unsigned char *s, unsigned int slen) {
    // 函数输入参数zl表示压缩列表首地址，p指向元素插入位置，s表示数据内容，slen表示数据长度，返回参数为		   压缩列表首地址*/
    return __ziplistInsert(zl,p,s,slen);
}
```

> ```c
> /* Return the total number of bytes used by the entry pointed to by 'p'. */
> unsigned int zipRawEntryLength(unsigned char *p) {
>     unsigned int prevlensize, encoding, lensize, len;
>     ZIP_DECODE_PREVLENSIZE(p, prevlensize);
>     ZIP_DECODE_LENGTH(p + prevlensize, encoding, lensize, len);
>     return prevlensize + lensize + len;
> }
> ```
>
> ![](https://mudongjing.github.io/gallery/redis/zip/change.png)

```c
/* Insert item at "p". */
/* 插入元素可以简要分为3个步骤：①将元素内容编码；②重新分配空间；③复制数据。下
   面分别讲解每个步骤的实现逻辑。*/
unsigned char *__ziplistInsert(unsigned char *zl, unsigned char *p, unsigned char *s, unsigned int slen) {
    // curlen表示插入元素前压缩列表的长度,
    // reqlen表示新插入元素的长度
    size_t curlen = intrev32ifbe(ZIPLIST_BYTES(zl)), reqlen, newlen;
    unsigned int prevlensize, prevlen = 0;
    size_t offset;
    // nextdiff表示entryX+1元素长度的变化，取值可能为0（长度不变）、4（长度增加4）或-4（长度减少4）
    int nextdiff = 0;
    unsigned char encoding = 0;
    long long value = 123456789; /* initialized to avoid warning. Using a value
                                    that is easy to see if for some reason
                                    we use it uninitialized. */
    zlentry tail;
    
	/* 编码即计算previous_entry_length字段、encoding字段和content字段的内容 */
    
    // 获取前一个元素的长度
    if (p[0] != ZIP_END) {
        ZIP_DECODE_PREVLEN(p, prevlensize, prevlen);
    } else {
        unsigned char *ptail = ZIPLIST_ENTRY_TAIL(zl);
        if (ptail[0] != ZIP_END) {
            prevlen = zipRawEntryLengthSafe(zl, curlen, ptail);
        }
    }

    // 判断是否需要编码
    /* encoding字段标识的是当前元素存储的数据类型和数据长度。编码时首先尝试将数据内容
	   解析为整数，如果解析成功，则按照压缩列表整数类型编码存储；如果解析失败，则按照压
	   缩列表字节数组类型编码存储。*/
    if (zipTryEncoding(s,slen,&value,&encoding)) {
        /* 'encoding' is set to the appropriate integer encoding */
        reqlen = zipIntSize(encoding);
    } else {
        /* 'encoding' is untouched, however zipStoreEntryEncoding will use the
         * string length to figure out how to encode it. */
        reqlen = slen;
    }
    
    /* We need space for both the length of the previous entry and
     * the length of the payload. */
    reqlen += zipStorePrevEntryLength(NULL,prevlen);
    reqlen += zipStoreEntryEncoding(NULL,encoding,slen);

   /* 当重新分配的空间减小时，realloc可能会将多余的空间回收，导致数据丢失。因此需要避免这种情况的发生，
	 即重新赋值nextdiff=0，同时使用forcelarge标记这种情况。*/
    int forcelarge = 0;
    nextdiff = (p[0] != ZIP_END) ? zipPrevLenByteDiff(p,reqlen) : 0;
    if (nextdiff == -4 && reqlen < 4) {
        nextdiff = 0;
        forcelarge = 1;
    }

    /* Store offset because a realloc may change the address of zl. */
    offset = p-zl;
    newlen = curlen+reqlen+nextdiff;
    zl = ziplistResize(zl,newlen);
    p = zl+offset;
    
    if (p[0] != ZIP_END) { 
        // 插入点之后的原有元素需要向后移动，空出需要的空间，移动的偏移量是待插入元素entryNEW的长度，移			动的数据块长度是位置P后所有元素的长度之和加上nextdiff的值，数据移动之后还需要更新后面一个元			 素的previous_entry_length 字段。*/
        memmove(p+reqlen,p-nextdiff,
                // 减一，zlend结束表示恒为0xFF，不需要移动
                curlen-offset-1+nextdiff);

        // 更新插入元素的previous_entry_length字段
        if (forcelarge)  
            // 后面元素的previous_entry_length字段依然占5个字节;但是插入元素的长度小于4字节
            zipStorePrevEntryLengthLarge(p+reqlen,reqlen);
        else   zipStorePrevEntryLength(p+reqlen,reqlen);

        // 更新zltail字段
        ZIPLIST_TAIL_OFFSET(zl) = intrev32ifbe(intrev32ifbe(ZIPLIST_TAIL_OFFSET(zl))+reqlen);

        /* When the tail contains more than one entry, we need to take
         * "nextdiff" in account as well. Otherwise, a change in the
         * size of prevlen doesn't have an effect on the *tail* offset. */
        assert(zipEntrySafe(zl, newlen, p+reqlen, &tail, 1));
        if (p[reqlen+tail.headersize+tail.len] != ZIP_END) {
            ZIPLIST_TAIL_OFFSET(zl) =
                intrev32ifbe(intrev32ifbe(ZIPLIST_TAIL_OFFSET(zl))+nextdiff);
        }
    } else { /* This element will be the new tail. */
        ZIPLIST_TAIL_OFFSET(zl) = intrev32ifbe(p-zl);
    }

    /* When nextdiff != 0, the raw length of the next entry has changed, so
     * we need to cascade the update throughout the ziplist */
    if (nextdiff != 0) {
        offset = p-zl;
        zl = __ziplistCascadeUpdate(zl,p+reqlen);
        p = zl+offset;
    }

    /* Write the entry */
    p += zipStorePrevEntryLength(p,prevlen);
    p += zipStoreEntryEncoding(p,encoding,slen);
    if (ZIP_IS_STR(encoding)) { memcpy(p,s,slen);} 
    else {zipSaveInteger(p,value,encoding);}
    //更新zllen字段
    ZIPLIST_INCR_LENGTH(zl,1);
    return zl;
}
```

![](https://mudongjing.github.io/gallery/redis/zip/copy.png)



## 删除

```c
// *p指向待删除元素的首地址（参数p同时可以作为输出参数）；返回参数为压缩列表首地址
unsigned char *ziplistDelete(unsigned char *zl, unsigned char **p) {
    size_t offset = *p-zl;
    zl = __ziplistDelete(zl,*p,1);

    /* Store pointer to current element in p, because ziplistDelete will
     * do a realloc which might result in a different "zl"-pointer.
     * When the delete direction is back to front, we might delete the last
     * entry and end up with "p" pointing to ZIP_END, so check this. */
    *p = zl+offset;
    return zl;
}
```

![](https://mudongjing.github.io/gallery/redis/zip/dele.png)

```c
// 可以同时删除多个元素，输入参数p指向的是首个待删除元素的地址；num表示待删除元素数目。
/* 删除元素同样可以简要分为三个步骤：①计算待删除元素的总长度；②数据复制；③重
   新分配空间。下面分别讨论每个步骤的实现逻辑。*/
unsigned char *__ziplistDelete(unsigned char *zl, unsigned char *p, unsigned int num) {
    unsigned int i, totlen, deleted = 0;
    size_t offset;
    int nextdiff = 0;
    zlentry first, tail;
    size_t zlbytes = intrev32ifbe(ZIPLIST_BYTES(zl));
	
    //解码第一个待删除元素
    zipEntry(p, &first); 
    //遍历所有待删除元素，同时指针p向后偏移
    for (i = 0; p[0] != ZIP_END && i < num; i++) {
        p += zipRawEntryLengthSafe(zl, zlbytes, p);
        deleted++;
    }

    assert(p >= first.p);
    totlen = p-first.p; //totlen即为待删除元素总长度
    
    // 现在，指针first与指针p之间的元素都是待删除的
    // 此后需要完成数据复制
    
    // 当指针p恰好指向zlend字段时，不再需要复制数据，只需要更新尾节点的偏移量即可
    
    if (totlen > 0) {
        uint32_t set_tail;
        if (p[0] != ZIP_END) {
            //计算元素entryN长度的变化量
            nextdiff = zipPrevLenByteDiff(p,first.prevrawlen);

            //更新元素entryN的previous_entry_length字段
            p -= nextdiff;
            assert(p >= first.p && p<zl+zlbytes-1);
            zipStorePrevEntryLength(p,first.prevrawlen);

            //更新zltail
            set_tail = intrev32ifbe(ZIPLIST_TAIL_OFFSET(zl))-totlen;

            /* When the tail contains more than one entry, we need to take
             * "nextdiff" in account as well. Otherwise, a change in the
             * size of prevlen doesn't have an effect on the *tail* offset. */
            assert(zipEntrySafe(zl, zlbytes, p, &tail, 1));
            
            /* 当entryN元素就是尾元素时，只需要更新一次尾元素的偏移量；但是当entryN元素不是尾元素且				   entryN元素的长度发生了改变时，尾元素偏移量还需要加上nextdiff的值。*/
            if (p[tail.headersize+tail.len] != ZIP_END) {set_tail = set_tail + nextdiff;}

            size_t bytes_to_move = zlbytes-(p-zl)-1;
            //数据复制
            memmove(first.p,p,bytes_to_move);
        } else {
            /* The entire tail was deleted. No need to move memory. */
            set_tail = (first.p-zl)-first.prevrawlen;
        }

        
        offset = first.p-zl;
        zlbytes -= totlen - nextdiff;
        zl = ziplistResize(zl, zlbytes);
        p = zl+offset;

        /* Update record count */
        ZIPLIST_INCR_LENGTH(zl,-deleted);

        /* Set the tail offset computed above */
        assert(set_tail <= zlbytes - ZIPLIST_END_SIZE);
        ZIPLIST_TAIL_OFFSET(zl) = intrev32ifbe(set_tail);

        /* When nextdiff != 0, the raw length of the next entry has changed, so
         * we need to cascade the update throughout the ziplist */
        if (nextdiff != 0) zl = __ziplistCascadeUpdate(zl,p);
    }
    return zl;
}
```

## 遍历

```c
// 分为向后查和向前查
unsigned char *ziplistNext(unsigned char *zl, unsigned char *p) {
    //zl参数无用；这里只是为了避免警告
    ((void) zl);
    size_t zlbytes = intrev32ifbe(ZIPLIST_BYTES(zl));

    /* "p" could be equal to ZIP_END, caused by ziplistDelete,
     * and we should return NULL. Otherwise, we should return NULL
     * when the *next* element is ZIP_END (there is no next entry). */
    if (p[0] == ZIP_END) { return NULL;}

    p += zipRawEntryLength(p);
    if (p[0] == ZIP_END) { return NULL; }

    zipAssertValidEntry(zl, zlbytes, p);
    return p;
}

/* Return pointer to previous entry in ziplist. */
unsigned char *ziplistPrev(unsigned char *zl, unsigned char *p) {
    unsigned int prevlensize, prevlen = 0;

    /* Iterating backwards from ZIP_END should return the tail. When "p" is
     * equal to the first element of the list, we're already at the head,
     * and should return NULL. */
    if (p[0] == ZIP_END) {
        p = ZIPLIST_ENTRY_TAIL(zl);
        return (p[0] == ZIP_END) ? NULL : p;
    } else if (p == ZIPLIST_ENTRY_HEAD(zl)) { return NULL;} 
    else {
        ZIP_DECODE_PREVLEN(p, prevlensize, prevlen);
        assert(prevlen > 0);
        p-=prevlen;
        size_t zlbytes = intrev32ifbe(ZIPLIST_BYTES(zl));
        zipAssertValidEntry(zl, zlbytes, p);
        return p;
    }
}
```

## 连锁更新

![](https://mudongjing.github.io/gallery/redis/zip/update.png)

> 当删除元素entryX时，元素entryX +1 的 前 驱 节 点 改 为 元 素 entryX-1， 长 度 为 512 字 节， 元 素 entryX +1 的previous_entry_length字段需要5字节才能存储元素entryX-1的长度，则元素entryX+1的长度需
> 要扩展至257字节；而由于元素entryX+1长度的增大，元素entryX+2的previous_entry_length字
> 段同样需要改变。依此类推，由于删除了元素entryX，之后的所有元素（entryX+1、entryX+2
> 等）的长度都必须扩展，而每次扩展都将重新分配内存，导致效率很低。压缩列表zl2中，插
> 入元素entryY时同样会出现这种情况，称为连锁更新。

# 字典【散列表】

c语言中没有实现的哈希表，因此，redis需要自己实现，而底层的实现就是数组加hash函数，与我们了解的hashMap一样，通过对字符串计算哈希值，得到数组的下标，再通过下标直接获得对应数组的值。

> 比如版本1的时候有个简单的hash函数
>
> ```c
> unsigned int dictGenHashFunction(const unsigned char *buf, int len) {
>     unsigned int hash = 5381;
>     while (len--)
>         hash = ((hash << 5) + hash) + (*buf++); /* hash * 33 + c */
>     return hash;
> }
> ```

![](https://mudongjing.github.io/gallery/redis/dict/dict.png)



## 结构

> 当已存入数据量将超过总容量时需进行扩容一倍。因此我们设计的字典数据结构在这就需要添加第2及第3个字段，分别为：①总容量——size字段；②已存入数据量——used字段
>
> ```c
> // 哈希表结构
> // Hash表的结构体整体占用32字节
> typedef struct dictht {
>     /*指针数组，用于存储键值对*/
>     dictEntry **table;
>     unsigned long size; // 数组大小,size是大于2的2次幂
>     unsigned long sizemask; // 掩码 size-1
>     unsigned long used;
>     // 获取的下标就是 hash值 & sizemask
> } dictht;
> ```

```c
// 哈希表节点
typedef struct dictEntry {
    void *key;
    union { // 这个联合体，用于存储实际的数据
        void *val;
        uint64_t u64;
        int64_t s64; // 存储过期时间
        double d;
    } v;
    struct dictEntry *next; // 冲突元素组成链表
} dictEntry;
```

```c
// 字典类型
typedef struct dictType {
    uint64_t (*hashFunction)(const void *key); // 哈希函数
    void *(*keyDup)(void *privdata, const void *key); // 键的复制函数
    void *(*valDup)(void *privdata, const void *obj); // 值的复制函数
    int (*keyCompare)(void *privdata, const void *key1, const void *key2);
    void (*keyDestructor)(void *privdata, void *key);
    void (*valDestructor)(void *privdata, void *obj); // 以上两个是销毁函数
    int (*expandAllowed)(size_t moreMem, double usedRatio);
} dictType;
```

![](https://mudongjing.github.io/gallery/redis/dict/construct.png)

```c
// 字典
typedef struct dict {
    dictType *type;
    void *privdata;
    dictht ht[2]; // k-v 哈希表， 虽然有两个元素，但一般情况下只会使用ht[0]，只有当该字典扩容、缩容需要进行rehash时，才会用到ht[1]
    long rehashidx; /*rehash标识。默认值为-1，代表没进行rehash操作；不为-1时，代表
					  正进行rehash操作，存储的值表示Hash表ht[0]的rehash操作进行到了哪个索引值*/
    int16_t pauserehash; /* If >0 rehashing is paused (<0 indicates coding error) */
} dict;
```

## 初始化

```c
dict *dictCreate(dictType *type, void *privDataPtr){
    dict *d = zmalloc(sizeof(*d));

    _dictInit(d,type,privDataPtr);
    return d;
}

int _dictInit(dict *d, dictType *type, void *privDataPtr){
    _dictReset(&d->ht[0]);
    _dictReset(&d->ht[1]);
    d->type = type;
    d->privdata = privDataPtr;
    d->rehashidx = -1;
    d->pauserehash = 0;
    return DICT_OK;
}
```



# 整数集合



# quciklist



# Stream



# 持久化



# 主从复制





