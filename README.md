# 使用方式
设置打印log的Controller目录或类的日志级别为`Debug`

如：`logging.level.site.ownw.demo.C=debug`

# 日志效果如下：
```
2021-07-28 20:19:41.380 DEBUG 16004 --- [76ccf347ab946f4] site.ownw.demo.C                         : HTTP请求处理开始:[handler=site.ownw.demo.C.test,ip=127.0.0.1,request=[p1,p2]]
2021-07-28 20:19:41.383 DEBUG 16004 --- [76ccf347ab946f4] site.ownw.demo.C                         : HTTP请求处理结束:[handler=site.ownw.demo.C.test,ip=127.0.0.1,response=String(p1 p2)]
```
