## 0.3.0

* Removed `Lambda.Proxy[I, O]`. One should use `Lambda.ApiProxy` instead. See #24;
* `ProxyRequest` renamed into `ApiProxyRequest`.
* `ApiProxyRequest` takes an additional type parameter. Pass `io.circe.Json` if you don't care or don't know what should go there.
* `ProxyResponse` renamed into `ApiProxyResponse`.  
* Fixed #24;
* Fixed some typos in some scaladocs.