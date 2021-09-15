

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

![](https://mudongjing.github.io/gallery/redis/dict/mem.png)

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

## 添加

```c
int dictAdd(dict *d, void *key, void *val){
    // 添加键，字典中键已存在则返回NULL，否则添加键至新节点中，返回新节点
    dictEntry *entry = dictAddRaw(d,key,NULL);
	/*键存在则返回错误*/
    if (!entry) return DICT_ERR;
    dictSetVal(d, entry, val);
    return DICT_OK;
}

```

```c
dictEntry *dictAddRaw(dict *d, void *key, dictEntry **existing){
    long index;
    dictEntry *entry;
    dictht *ht;
	/*该字典是否在进行rehash操作中,是则执行一次rehash*/
    if (dictIsRehashing(d)) _dictRehashStep(d);

    /*查找键，找到则直接返回-1, 并把老节点存入existing字段，否则把新节点的索引值返回。如果遇到Hash表容	    量不足，则进行扩容*/
    // dictHashKey(d,key) 调用该字典的Hash函数得到键的Hash值
    if ((index = _dictKeyIndex(d, key, dictHashKey(d,key), existing)) == -1) return NULL;

    /*是否进行rehash操作中,是则插入至散列表ht[1]中，否则插入散列表ht[0] */
    ht = dictIsRehashing(d) ? &d->ht[1] : &d->ht[0];

    /*申请新节点内存，插入散列表中，给新节点存入键信息*/
    entry = zmalloc(sizeof(*entry));
    entry->next = ht->table[index];
    ht->table[index] = entry;
    ht->used++;

    /* Set the hash entry fields. */
    dictSetKey(d, entry, key);
    return entry;
}
#define dictSetVal(d, entry, _val_) do { 
    if ((d)->type->valDup) (entry)->v.val = (d)->type->valDup((d)->privdata, _val_); 
    else  (entry)->v.val = (_val_); 
} while(0)
    
static long _dictKeyIndex(dict *d, const void *key, uint64_t hash, dictEntry **existing){
    unsigned long idx, table;
    dictEntry *he;
    if (existing) *existing = NULL;

    /* Expand the hash table if needed */
    if (_dictExpandIfNeeded(d) == DICT_ERR) return -1;
    for (table = 0; table <= 1; table++) {
        idx = hash & d->ht[table].sizemask; // 用键的Hash值与字典掩码取与，得到索引值
        /* Search if this slot does not already contain the given key */
        he = d->ht[table].table[idx];
        while(he) {
            if (key==he->key || dictCompareKeys(d, key, he->key)) {
                if (existing) *existing = he;
                return -1;
            }
            he = he->next;
        }
        if (!dictIsRehashing(d)) break;
    }
    return idx;
}
```

## 扩容

![](https://mudongjing.github.io/gallery/redis/dict/expansion.png)

```c
/*扩容主要流程为：
  ①申请一块新内存，初次申请时默认容量大小为4个dictEntry；非初次申请时，申请内存的大小则为当前Hash表容量    的一倍。
  ②把新申请的内存地址赋值给ht[1]，并把字典的rehashidx标识由-1改为0，表示之后需要进行rehash操作。*/
int dictExpand(dict *d, unsigned long size) {
    return _dictExpand(d, size, NULL);
}
/* 扩容后，字典容量及掩码值会发生改变，同一个键与掩码经位运算后得到的索引值就会发生改变，从而导致根据键查 找不到值的情况。
   解决这个问题的方法是，新扩容的内存放到一个全新的Hash表中（ht[1]），并给字典打上在进行rehash操作中的标 识（即rehashidx!=-1）。
   此后，新添加的键值对都往新的Hash表中存储；而修改、删除、查找操作需要在ht[0]、ht[1]中进行检查，然后再决定去对哪个Hash表操作。除此之外，还需要把老Hash表（ht[0]）中的数据重新计算索引值后全部迁移插入到新的Hash表(ht[1])中，此迁移过程称作rehash，*/
```

```c
int _dictExpand(dict *d, unsigned long size, int* malloc_failed){
    if (malloc_failed) *malloc_failed = 0;

    if (dictIsRehashing(d) || d->ht[0].used > size) return DICT_ERR;

    dictht n; /* the new hash table */
    unsigned long realsize = _dictNextPower(size); /*重新计算扩容后的值，必须为2的N次方幂*/

    /* Rehashing to the same table size is not useful. */
    if (realsize == d->ht[0].size) return DICT_ERR;

    /* Allocate the new hash table and initialize all pointers to NULL */
    n.size = realsize;
    n.sizemask = realsize-1;
    if (malloc_failed) {
        n.table = ztrycalloc(realsize*sizeof(dictEntry*));
        *malloc_failed = n.table == NULL;
        if (*malloc_failed) return DICT_ERR;
    } 
    else  n.table = zcalloc(realsize*sizeof(dictEntry*));

    n.used = 0;

    /* Is this the first initialization? If so it's not really a rehashing
     * we just set the first hash table so that it can accept keys. */
    if (d->ht[0].table == NULL) {
        d->ht[0] = n;
        return DICT_OK;
    }

    /* Prepare a second hash table for incremental rehashing */
    d->ht[1] = n; // 扩容后的新内存放入ht[1]中
    d->rehashidx = 0; // 非默认的-1,表示需进行rehash
    return DICT_OK;
}

static unsigned long _dictNextPower(unsigned long size){
    unsigned long i = DICT_HT_INITIAL_SIZE;

    if (size >= LONG_MAX) return LONG_MAX + 1LU;
    while(1) {
        if (i >= size)  return i;
        i *= 2;
    }
}
```

![](https://mudongjing.github.io/gallery/redis/dict/refresh.png)

### rehash

rehash除了扩容时会触发，缩容时也会触发。Redis整个rehash的实现，主要分为如下几步
完成。

1. 给Hash表ht[1]申请足够的空间；扩容时空间大小为当前容量*2，即d->ht[0].used*2；
   当使用量不到总空间10%时，则进行缩容。缩容时空间大小则为能恰好包含d->ht[0].used个节
   点的2^N次方幂整数，并把字典中字段rehashidx标识为0。
2. 进行rehash操作调用的是dictRehash函数，重新计算ht[0]中每个键的Hash值与索引值
   （重新计算就叫rehash），依次添加到新的Hash表ht[1]，并把老Hash表中该键值对删除。把字
   典中字段rehashidx字段修改为Hash表ht[0]中正在进行rehash操作节点的索引值。
3. rehash操作后，清空ht[0]，然后对调一下ht[1]与ht[0]的值，并把字典中rehashidx字段
   标识为-1。

> 执行插入、删除、查找、修改等操作前，都先判断当前字典rehash操作是否在进行中，进
> 行中则调用dictRehashStep函数进行rehash操作（每次只对1个节点进行rehash操作，共执行1
> 次）。
>
> 除这些操作之外，当服务空闲时，如果当前字典也需要进行rehsh操作，则会调用
> incrementallyRehash函数进行批量rehash操作（每次对100个节点进行rehash操作，共执行1毫
> 秒）。
>
> 在经历N次rehash操作后，整个ht[0]的数据都会迁移到ht[1]中，这样做的好处就把是本
> 应集中处理的时间分散到了上百万、千万、亿次操作中，所以其耗时可忽略不计。



## 查找

```c
dictEntry *dictFind(dict *d, const void *key){
    dictEntry *he;
    uint64_t h, idx, table;

    if (dictSize(d) == 0) return NULL; /* dict is empty */
    if (dictIsRehashing(d)) _dictRehashStep(d);
    h = dictHashKey(d, key);// 获取键值的hash值
    for (table = 0; table <= 1; table++) { // 遍历查找Hash表 ht[0]与ht[1]
        idx = h & d->ht[table].sizemask; // 根据Hash值获取到对应的索引值
        he = d->ht[table].table[idx];
        while(he) {
            if (key==he->key || dictCompareKeys(d, key, he->key))
                return he;
            he = he->next;
        }
        if (!dictIsRehashing(d)) return NULL; // 如果未进行rehash操作，则只读取ht[0]
    }
    return NULL;
}
```



## 修改

```c
void dbOverwrite(redisDb *db, robj *key, robj *val) {
    dictEntry *de = dictFind(db->dict,key->ptr); // 查找键存在与否，返回存在的节点
    serverAssertWithInfo(NULL,key,de != NULL); // 不存在则中断执行
    dictEntry auxentry = *de;
    robj *old = dictGetVal(de); // 获取老节点的val字段值
    if (server.maxmemory_policy & MAXMEMORY_FLAG_LFU) {
        val->lru = old->lru;
    }
    /* Although the key is not really deleted from the database, we regard 
    overwrite as two steps of unlink+add, so we still need to call the unlink 
    callback of the module. */
    moduleNotifyKeyUnlink(key,old);
    dictSetVal(db->dict, de, val); // 给节点设置新的值

    if (server.lazyfree_lazy_server_del) {
        freeObjAsync(key,old);
        dictSetVal(db->dict, &auxentry, NULL);
    }

    dictFreeVal(db->dict, &auxentry); // 释放节点中旧val内存
}
```

## 删除

```c
int dictDelete(dict *ht, const void *key) {
    return dictGenericDelete(ht,key,0) ? DICT_OK : DICT_ERR;
}
```

```c
static dictEntry *dictGenericDelete(dict *d, const void *key, int nofree) {
    uint64_t h, idx;
    dictEntry *he, *prevHe;
    int table;

    if (d->ht[0].used == 0 && d->ht[1].used == 0) return NULL;

    if (dictIsRehashing(d)) _dictRehashStep(d);
    h = dictHashKey(d, key);

    for (table = 0; table <= 1; table++) {
        idx = h & d->ht[table].sizemask;
        he = d->ht[table].table[idx];
        prevHe = NULL;
        while(he) {
            if (key==he->key || dictCompareKeys(d, key, he->key)) {
                /* Unlink the element from the list */
                if (prevHe) prevHe->next = he->next;
                else  d->ht[table].table[idx] = he->next;
                if (!nofree) {
                    dictFreeKey(d, he);
                    dictFreeVal(d, he);
                    zfree(he);
                }
                d->ht[table].used--;
                return he;
            }
            prevHe = he;
            he = he->next;
        }
        if (!dictIsRehashing(d)) break;
    }
    return NULL; /* not found */
}
```

## 遍历

### 迭代器

```c
typedef struct dictIterator {
    dict *d; //迭代的字典
    long index; //当前迭代到Hash表中哪个索引值
    //table用于表示当前正在迭代的Hash表,即ht[0]与ht[1]，safe用于表示当前创建的是否为安全迭代器
    int table, safe; 
    dictEntry *entry, *nextEntry; //当前节点，下一个节点
    //字典的指纹，当字典未发生改变时，该值不变，发生改变时则值也随着改变
    //该字段的值为字典（dict结构体）中所有字段值组合在一起生成的Hash值，所以当字典中数据发生任何变化时，		 其值都会不同
    long long fingerprint;
} dictIterator;
```

 ```c
 // 指纹生成函数
 long long dictFingerprint(dict *d) {
     long long integers[6], hash = 0;
     int j;
 
     integers[0] = (long) d->ht[0].table;
     integers[1] = d->ht[0].size;
     integers[2] = d->ht[0].used;
     integers[3] = (long) d->ht[1].table;
     integers[4] = d->ht[1].size;
     integers[5] = d->ht[1].used;
 
     for (j = 0; j < 6; j++) {
         hash += integers[j];
         /* For the hashing step we use Tomas Wang's 64 bit integer hash. */
         hash = (~hash) + (hash << 21); // hash = (hash << 21) - hash - 1;
         hash = hash ^ (hash >> 24);
         hash = (hash + (hash << 3)) + (hash << 8); // hash * 265
         hash = hash ^ (hash >> 14);
         hash = (hash + (hash << 2)) + (hash << 4); // hash * 21
         hash = hash ^ (hash >> 28);
         hash = hash + (hash << 31);
     }
     return hash;
 }
 ```

> Redis如何解决增删数据的同时不出现读取数据重复的问题。Redis为单进程单线程模式，不存在两个命令同时执行的情况，因此只有当执行的命令在遍历的同时删除了数据，才会触发前面的问题。我们把迭代器
> 遍历数据分为两类：
>
> 1. 普通迭代器，只遍历数据；
> 2. 安全迭代器，遍历的同时删除数据。
>
> ![](https://mudongjing.github.io/gallery/redis/dict/iter-init.png)
>
> ![](https://mudongjing.github.io/gallery/redis/dict/itera.png)



### 间断遍历

```c
/* 变量d是当前迭代的字典；变量v标识迭代开始的游标（即Hash表中数组索引），每次遍历后会返回新的游标值，整个    遍历过程都是围绕这个游标值的改动进行，来保证所有的数据能被遍历到；fn是函数指针，每遍历一个节点则调用该    函数处理；bucketfn函数在整理碎片时调用；privdata是回调函数fn所需参数。*/
unsigned long dictScan(dict *d, unsigned long v, dictScanFunction *fn,
                       dictScanBucketFunction* bucketfn, void *privdata){
    dictht *t0, *t1;
    const dictEntry *de, *next;
    unsigned long m0, m1;
    if (dictSize(d) == 0) return 0;

    /* This is needed in case the scan callback tries to do dictFind or alike. */
    dictPauseRehashing(d);

    if (!dictIsRehashing(d)) {
        t0 = &(d->ht[0]);
        m0 = t0->sizemask;

        /* Emit entries at cursor */
        if (bucketfn) bucketfn(privdata, &t0->table[v & m0]);
        de = t0->table[v & m0];
        while (de) {
            next = de->next;
            fn(privdata, de);
            de = next;
        }
        /* Set unmasked bits so incrementing the reversed cursor
         * operates on the masked bits */
        v |= ~m0;

        /* Increment the reverse cursor */
        v = rev(v);
        v++;
        v = rev(v);
    } else {
        t0 = &d->ht[0];
        t1 = &d->ht[1];

        /* Make sure t0 is the smaller and t1 is the bigger table */
        if (t0->size > t1->size) {
            t0 = &d->ht[1];
            t1 = &d->ht[0];
        }

        m0 = t0->sizemask;
        m1 = t1->sizemask;

        /* Emit entries at cursor */
        if (bucketfn) bucketfn(privdata, &t0->table[v & m0]);
        de = t0->table[v & m0];
        while (de) {
            next = de->next;
            fn(privdata, de);
            de = next;
        }

        /* Iterate over indices in larger table that are the expansion
         * of the index pointed to by the cursor in the smaller table */
        do {
            /* Emit entries at cursor */
            if (bucketfn) bucketfn(privdata, &t1->table[v & m1]);
            de = t1->table[v & m1];
            while (de) {
                next = de->next;
                fn(privdata, de);
                de = next;
            }

            /* Increment the reverse cursor not covered by the smaller mask.*/
            v |= ~m1; v = rev(v); 
            v++;  v = rev(v);
            /* Continue while bits covered by mask difference is non-zero */
        } while (v & (m0 ^ m1));
    }
    dictResumeRehashing(d);
    return v;
}
```

> dictScan函数间断遍历字典过程中会遇到如下3种情况。
>
> 1. 从迭代开始到结束，散列表没有进行rehash操作。
> 2. 从迭代开始到结束，散列表进行了扩容或缩容操作，且恰好为两次迭代间隔期间完成了rehash操作。
> 3. 从迭代开始到结束，某次或某几次迭代时散列表正在进行rehash操作。

# 整数集合

整数集合（intset）是一个有序的、存储整型数据的结构。我们知道Redis是一个内存数据库，所以必须考虑如何能够高效地利用内存。当Redis集合类型的元素都是整数并且都处在64位有符号整数范围之内时，使用该结构体存储。

在两种情况下，底层编码会发生转换。一种情况为当元素个数超过一定数量之后（默认值为512），即使元素类型仍然是整型，也会将编码转换为hashtable，该值由如下配置项决定：

`set-max-intset-entries 512`
另一种情况为当增加非整型变量时，例如在集合中增加元素'a'后，testSet的底层编码从intset转换为hashtable。

```c
typedef struct intset {
    uint32_t encoding; //编码类型，决定每个元素占用几个字节
   /*INTSET_ENC_INT16：当元素值都位于INT16_MIN和INT16_MAX之间时使用。该编码方式为每个元素占用2个字节。
	 INTSET_ENC_INT32：当元素值位于INT16_MAX到INT32_MAX或者INT32_MIN到INT16_MIN之间时使用。该编码方式为每个元素占用4个字节。
	 INTSET_ENC_INT64：当元素值位于INT32_MAX到INT64_MAX或者INT64_MIN到INT32_MIN之间时使用。该编码方式为每个元素占用8个字节。*/
    uint32_t length; //元素个数
    int8_t contents[];
} intset;
```

## 查询

```c
uint8_t intsetFind(intset *is, int64_t value) {
    uint8_t valenc = _intsetValueEncoding(value);
    return valenc <= intrev32ifbe(is->encoding) && intsetSearch(is,value,NULL);
}
```

```c
// intset是按从小到大有序排列的，使用二分查找
static uint8_t intsetSearch(intset *is, int64_t value, uint32_t *pos) {
    int min = 0, max = intrev32ifbe(is->length)-1, mid = -1;
    int64_t cur = -1;

    /* The value can never be found when the set is empty */
    if (intrev32ifbe(is->length) == 0) {
        if (pos) *pos = 0;
        return 0;
    } else {
        /* Check for the case where we know we cannot find the value,
         * but do know the insert position. */
        if (value > _intsetGet(is,max)) {
            if (pos) *pos = intrev32ifbe(is->length);
            return 0;
        } else if (value < _intsetGet(is,0)) {
            if (pos) *pos = 0;
            return 0;
        }
    }

    while(max >= min) {
        mid = ((unsigned int)min + (unsigned int)max) >> 1;
        cur = _intsetGet(is,mid);
        if (value > cur) { min = mid+1;
        } else if (value < cur) { max = mid-1;
        } else { break;
        }
    }

    if (value == cur) {
        if (pos) *pos = mid;
        return 1;
    } else {
        if (pos) *pos = min;
        return 0;
    }
}
```

## 添加

```c
// 该函数根据插入值的编码类型和当前intset的编码类型决定是直接插入还是先进行intset升级再执行插入
intset *intsetAdd(intset *is, int64_t value, uint8_t *success) {
    uint8_t valenc = _intsetValueEncoding(value); //获取添加元素的编码值
    uint32_t pos;
    if (success) *success = 1;

    /* Upgrade encoding if necessary. If we need to upgrade, we know that
     * this value should be either appended (if > 0) or prepended (if < 0),
     * because it lies outside the range of existing values. */
    if (valenc > intrev32ifbe(is->encoding)) {
       //调用intsetUpgradeAndAdd进行升级后添加
        return intsetUpgradeAndAdd(is,value);
    } else {
       //否则先进行查重,如果已经存在该元素，直接返回
        if (intsetSearch(is,value,&pos)) {
            if (success) *success = 0;
            return is;
        }

        is = intsetResize(is,intrev32ifbe(is->length)+1);//首先将intset占用内存扩容
        //如果插入元素在intset中间位置，调用intsetMoveTail给元素挪出空间
        if (pos < intrev32ifbe(is->length)) intsetMoveTail(is,pos,pos+1);
    }

    _intsetSet(is,pos,value);//保存元素
    is->length = intrev32ifbe(intrev32ifbe(is->length)+1);//修改intset的长度，将其加1
    return is;
}
```

## 删除

```c
//该函数查找需要删除的元素然后通过内存地址的移动直接将该元素覆盖掉
intset *intsetRemove(intset *is, int64_t value, int *success) {
    uint8_t valenc = _intsetValueEncoding(value);
    uint32_t pos;
    if (success) *success = 0;
	//待删除元素编码必须小于等于intset编码并且查找到该元素，才会执行删除操作
    if (valenc <= intrev32ifbe(is->encoding) && intsetSearch(is,value,&pos)) {
        uint32_t len = intrev32ifbe(is->length);

        /* We know we can delete */
        if (success) *success = 1;

        //如果待删除元素位于中间位置,则调用intsetMoveTail直接覆盖掉该元素
		//如果待删除元素位于intset末尾,则intset收缩内存后直接将其丢弃
        if (pos < (len-1)) intsetMoveTail(is,pos+1,pos);
        is = intsetResize(is,len-1);
        is->length = intrev32ifbe(len-1);//修改intset的长度，将其减1
    }
    return is;
}
```



# quciklist

> 在引入quicklist之前，Redis采用压缩链表（ziplist）以及双向链表（adlist）作为List的底层实现。当元素个数比较少并且元素长度比较小时，Redis采用ziplist作为其底层存储；当任意一个条件不满足时，Redis采用adlist作为底层存储结构。这么做的主要原因是，当元素长度较小时，采用ziplist可以有效节省存储空间，但ziplist的存储空间是连续的，当元素个数比较多时，修改元素时，必须重新分配存储空间，这无疑会影响Redis的执行效率，故而采用一般的双向链表。
> quicklist是综合考虑了时间效率与空间效率引入的新型数据结构，quicklist由List和ziplist结合而成。
>
> > quicklist是一个双向链表，链表中的每个节点是一个ziplist结构。quicklist可以看成是用双向链表将若干小型的ziplist连接到一起组成的一种数据结构。当ziplist节点个数过多，quicklist退化为双向链表，一
> > 个极端的情况就是每个ziplist节点只包含一个entry，即只有一个元素。当ziplist元素个数过少
> > 时，quicklist可退化为ziplist，一种极端的情况就是quicklist中只有一个ziplist节点。

![](https://mudongjing.github.io/gallery/redis/quicklist/construct.png)

```c
typedef struct quicklist {
    quicklistNode *head;
    quicklistNode *tail;
    unsigned long count;        /* total count of all entries in all ziplists */
    unsigned long len;          /* number of quicklistNodes */
    int fill : QL_FILL_BITS;    /* 指明每个quicklistNode中ziplist长度，当fill为正数时，表明每									个ziplist最多含有的数据项数，当fill为负数时，如下标所示*/
    unsigned int compress : QL_COMP_BITS; /* depth of end nodes not to compress;0=off */
    unsigned int bookmark_count: QL_BM_BITS;
    quicklistBookmark bookmarks[];
} quicklist;
```

| fill | 含义                 |
| ---- | -------------------- |
| -1   | ziplist节点最大为4KB |
| -2   | 8KB                  |
| -3   | 16                   |
| -4   | 32                   |
| -5   | 64                   |

![](https://mudongjing.github.io/gallery/redis/quicklist/zip.png)

```c
typedef struct quicklistNode {
    struct quicklistNode *prev;
    struct quicklistNode *next;
    unsigned char *zl; // 指向该节点对应的ziplist结构
    unsigned int sz;           //代表整个ziplist结构的大小
    unsigned int count : 16;     /* count of items in ziplist */
    unsigned int encoding : 2;  //代表采用的编码方式：1代表是原生的，2代表使用LZF进行压缩
    unsigned int container : 2;  //为quicklistNode节点zl指向的容器类型：1代表none，                                                    2代表使用ziplist存储数据
    unsigned int recompress : 1; //代表这个节点之前是否是压缩节点，若是，则在使用压缩节点前先进行解压								   缩，使用后需要重新压缩，此外为1，代表是压缩节点
    unsigned int attempted_compress : 1; /* node can't compress; too small */
    unsigned int extra : 10; /* more bits to steal for future usage */
} quicklistNode;
```

## 初始化

```c
quicklist *quicklistCreate(void) {
    struct quicklist *quicklist;

    quicklist = zmalloc(sizeof(*quicklist));
    quicklist->head = quicklist->tail = NULL;
    quicklist->len = 0;
    quicklist->count = 0;
    quicklist->compress = 0;
    quicklist->fill = -2;
    quicklist->bookmark_count = 0;
    return quicklist;
}
```

## 添加

```c
int quicklistPushHead(quicklist *quicklist, void *value, size_t sz) {
    quicklistNode *orig_head = quicklist->head;
    // 当ziplist已经包含节点时，在ziplist头部插入数据可能导致ziplist的连锁更新
    if (likely(_quicklistNodeAllowInsert(quicklist->head, quicklist->fill, sz))) {
        //头部节点仍然可以插入
        quicklist->head->zl = ziplistPush(quicklist->head->zl, value, sz, ZIPLIST_HEAD);
        quicklistNodeUpdateSz(quicklist->head);
    } else {//头部节点不可以继续插入, 新建quicklistNode, ziplist
        quicklistNode *node = quicklistCreateNode();
        node->zl = ziplistPush(ziplistNew(), value, sz, ZIPLIST_HEAD);
        quicklistNodeUpdateSz(node);
        //将新建的quicklistNode插入到quicklist结构体中
        _quicklistInsertNodeBefore(quicklist, quicklist->head, node);
    }
    quicklist->count++;
    quicklist->head->count++;
    return (orig_head != quicklist->head);
}
```

```c
int quicklistPushTail(quicklist *quicklist, void *value, size_t sz) {
    quicklistNode *orig_tail = quicklist->tail;
    if (likely(_quicklistNodeAllowInsert(quicklist->tail, quicklist->fill, sz))) {
        quicklist->tail->zl = ziplistPush(quicklist->tail->zl, value, sz, ZIPLIST_TAIL);
        quicklistNodeUpdateSz(quicklist->tail);
    } else {
        quicklistNode *node = quicklistCreateNode();
        node->zl = ziplistPush(ziplistNew(), value, sz, ZIPLIST_TAIL);
        quicklistNodeUpdateSz(node);
        _quicklistInsertNodeAfter(quicklist, quicklist->tail, node);
    }
    quicklist->count++;
    quicklist->tail->count++;
    return (orig_tail != quicklist->tail);
}
```

### 插入

![](https://mudongjing.github.io/gallery/redis/quicklist/insert.png)

```c
REDIS_STATIC void _quicklistInsert(quicklist *quicklist, quicklistEntry *entry,
                                   void *value, const size_t sz, int after) {
    int full = 0, at_tail = 0, at_head = 0, full_next = 0, full_prev = 0;
    int fill = quicklist->fill;
    quicklistNode *node = entry->node;
    quicklistNode *new_node = NULL;

    if (!node) {
        /* we have no reference node, so let's create only node in the list */
        D("No node given!");
        new_node = quicklistCreateNode();
        new_node->zl = ziplistPush(ziplistNew(), value, sz, ZIPLIST_HEAD);
        __quicklistInsertNode(quicklist, NULL, new_node, after);
        new_node->count++;
        quicklist->count++;
        return;
    }

    /* Populate accounting flags for easier boolean checks later */
    if (!_quicklistNodeAllowInsert(node, fill, sz)) {
        D("Current node is full with count %d with requested fill %lu",node->count, fill);
        full = 1;
    }

    if (after && (entry->offset == node->count)) {
        D("At Tail of current ziplist");
        at_tail = 1;
        if (!_quicklistNodeAllowInsert(node->next, fill, sz)) {
            D("Next node is full too.");
            full_next = 1;
        }
    }

    if (!after && (entry->offset == 0)) {
        D("At Head");
        at_head = 1;
        if (!_quicklistNodeAllowInsert(node->prev, fill, sz)) {
            D("Prev node is full too.");
            full_prev = 1;
        }
    }

    /* Now determine where and how to insert the new element */
    if (!full && after) {
        D("Not full, inserting after current position.");
        quicklistDecompressNodeForUse(node);
        unsigned char *next = ziplistNext(node->zl, entry->zi);
        if (next == NULL) { node->zl = ziplistPush(node->zl, value, sz, ZIPLIST_TAIL);
        } else { node->zl = ziplistInsert(node->zl, next, value, sz);}
        node->count++;
        quicklistNodeUpdateSz(node);
        quicklistRecompressOnly(quicklist, node);
    } else if (!full && !after) {
        D("Not full, inserting before current position.");
        quicklistDecompressNodeForUse(node);
        node->zl = ziplistInsert(node->zl, entry->zi, value, sz);
        node->count++;
        quicklistNodeUpdateSz(node);
        quicklistRecompressOnly(quicklist, node);
    } else if (full && at_tail && node->next && !full_next && after) {
        /* If we are: at tail, next has free space, and inserting after:
         *   - insert entry at head of next node. */
        D("Full and tail, but next isn't full; inserting next node head");
        new_node = node->next;
        quicklistDecompressNodeForUse(new_node);
        new_node->zl = ziplistPush(new_node->zl, value, sz, ZIPLIST_HEAD);
        new_node->count++;
        quicklistNodeUpdateSz(new_node);
        quicklistRecompressOnly(quicklist, new_node);
    } else if (full && at_head && node->prev && !full_prev && !after) {
        /* If we are: at head, previous has free space, and inserting before:
         *   - insert entry at tail of previous node. */
        D("Full and head, but prev isn't full, inserting prev node tail");
        new_node = node->prev;
        quicklistDecompressNodeForUse(new_node);
        new_node->zl = ziplistPush(new_node->zl, value, sz, ZIPLIST_TAIL);
        new_node->count++;
        quicklistNodeUpdateSz(new_node);
        quicklistRecompressOnly(quicklist, new_node);
    } else if (full && ((at_tail && node->next && full_next && after) ||
                        (at_head && node->prev && full_prev && !after))) {
        /* If we are: full, and our prev/next is full, then:
         *   - create new node and attach to quicklist */
        D("\tprovisioning new node...");
        new_node = quicklistCreateNode();
        new_node->zl = ziplistPush(ziplistNew(), value, sz, ZIPLIST_HEAD);
        new_node->count++;
        quicklistNodeUpdateSz(new_node);
        __quicklistInsertNode(quicklist, node, new_node, after);
    } else if (full) {
        /* else, node is full we need to split it. */
        /* covers both after and !after cases */
        D("\tsplitting node...");
        quicklistDecompressNodeForUse(node);
        new_node = _quicklistSplitNode(node, entry->offset, after);
        new_node->zl = ziplistPush(new_node->zl, value, sz,
                                   after ? ZIPLIST_HEAD : ZIPLIST_TAIL);
        new_node->count++;
        quicklistNodeUpdateSz(new_node);
        __quicklistInsertNode(quicklist, node, new_node, after);
        _quicklistMergeNodes(quicklist, node);
    }
    quicklist->count++;
}
```



# Stream

这是用于消息队列。



# 持久化



# 主从复制





