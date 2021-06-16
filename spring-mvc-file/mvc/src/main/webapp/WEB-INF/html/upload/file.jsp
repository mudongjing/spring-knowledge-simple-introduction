<%--
  Created by IntelliJ IDEA.
  User: dongmu
  Date: 21.6.11
  Time: 下午 08:04
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>文件上传</title>
</head>
<body>
<form method="POST" enctype="multipart/form-data" action="fileupload">
    <input type="file" name="upfile">
    <input type="submit" value="Upload">
</form>
</body>
</html>
