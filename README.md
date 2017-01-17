# Bridge

Bridge is a simple but powerful HTTP networking library for Android. It features a Fluent chainable API,
powered by Java/Android's URLConnection classes for maximum compatibility and speed.

# Table of Contents

### Traditional

1. [Gradle Dependency](https://github.com/afollestad/bridge#gradle-dependency)
	1. [Repository](https://github.com/afollestad/bridge#repository)
	2. [Dependency](https://github.com/afollestad/bridge#dependency)
2. [Requests](https://github.com/afollestad/bridge#requests)
	1. [Request Basics](https://github.com/afollestad/bridge#request-basics)
	2. [Request Headers](https://github.com/afollestad/bridge#request-headers)
	3. [Request Bodies](https://github.com/afollestad/bridge#request-bodies)
		1. [Plain Bodies](https://github.com/afollestad/bridge#plain-bodies)
		2. [Form Bodies](https://github.com/afollestad/bridge#plain-bodies)
		3. [MultipartForm Bodies](https://github.com/afollestad/bridge#plain-bodies)
	4. [Streaming Bodies](https://github.com/afollestad/bridge#plain-bodies)
	4. [Info Callbacks](https://github.com/afollestad/bridge#info-callbacks)
3. [Responses](https://github.com/afollestad/bridge#responses)
	1. [Response Basics](https://github.com/afollestad/bridge#response-basics)
	2. [Response Bodies](https://github.com/afollestad/bridge#response-bodies)
4. [Error Handling](https://github.com/afollestad/bridge#error-handling)
5. [Async](https://github.com/afollestad/bridge#async)
	1. [Async Requests](https://github.com/afollestad/bridge#async-requests)
	2. [Duplicate Avoidance](https://github.com/afollestad/bridge#duplicate-avoidance)
	3. [Upload Progress](https://github.com/afollestad/bridge#upload-progress)
	4. [Download Progress](https://github.com/afollestad/bridge#download-progress)
6. [Request Cancellation](https://github.com/afollestad/bridge#request-cancellation)
	1. [Cancelling Single Requests](https://github.com/afollestad/bridge#cancelling-single-requests)
	2. [Cancelling Multiple Requests](https://github.com/afollestad/bridge#cancelling-multiple-requests)
	3. [Preventing Cancellation](https://github.com/afollestad/bridge#preventing-cancellation)
7. [Validation](https://github.com/afollestad/bridge#validation)
8. [Configuration](https://github.com/afollestad/bridge#configuration)
	1. [Host Configuration](https://github.com/afollestad/bridge#host-configuration)
	2. [Default Headers](https://github.com/afollestad/bridge#default-headers)
	3. [Timeout Configuration](https://github.com/afollestad/bridge#timeout-configuration)
	4. [Buffer Size](https://github.com/afollestad/bridge#buffer-size)
	5. [Logging](https://github.com/afollestad/bridge#logging)
	6. [Global Validators](https://github.com/afollestad/bridge#global-validators)
9. [Cleanup](https://github.com/afollestad/bridge#cleanup)

### Conversion

1. [Request Conversion](https://github.com/afollestad/bridge#request-conversion)
	1. [JSON Request Conversion](https://github.com/afollestad/bridge#json-request-conversion)
	2. [Dot Notation](https://github.com/afollestad/bridge#dot-notation)
	3. [Request Conversion API](https://github.com/afollestad/bridge#request-conversion-api)
2. [Response Conversion](https://github.com/afollestad/bridge#response-conversion)
	1. [JSON Response Conversion](https://github.com/afollestad/bridge#json-response-conversion)
	2. [Dot Notation](https://github.com/afollestad/bridge#dot-notation-1)
	3. [Response Conversion API](https://github.com/afollestad/bridge#response-conversion-api)

---

# Gradle Dependency

[ ![jCenter](https://api.bintray.com/packages/drummer-aidan/maven/bridge/images/download.svg) ](https://bintray.com/drummer-aidan/maven/bridge/_latestVersion)
[![Build Status](https://travis-ci.org/afollestad/bridge.svg)](https://travis-ci.org/afollestad/bridge)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0.html)

The Gradle dependency is available via [jCenter](https://bintray.com/drummer-aidan/maven/bridge/view).
jCenter is the default Maven repository used by Android Studio.

### Dependency

Add this to your module's `build.gradle` file:

```gradle
dependencies {
	// ... other dependencies

	compile 'com.afollestad:bridge:3.3.0'
}
```

---

# Requests

The request API in Bridge is very easy to use. 

### Request Basics

The code below will request Google's homepage:

```java
Request request = Bridge
	.get("https://www.google.com")
	.request();
```

---

Bridge allows you to pass format args into request URLs (this applies to `get()`, `post()`, `put()`, `delete()`, etc.):

```java
Request request = Bridge
	.get("https://www.google.com/search?q=%s", searchQuery)
	.request();
```

There are two advantages to doing this when your requests require query parameters: 
code readability is improved, no string. concatenation is necessary. The contents of
the `searchQuery` variable are automatically URL encoded for you, e.g spaces are 
replaced with `%20`.

### Request Headers

Adding or changing request headers is pretty simple:

```java
Request request = Bridge
	.get("https://www.google.com")
	.header("User-Agent", "My App!")
	.header("CustomHeader", "HelloWorld")
	.request();
```

If you had the need to do so, you could also set a Map of headers with
the `headers(Map<String, Object>)` method.

**Note**: the [Configuration](https://github.com/afollestad/bridge#configuration)
goes over how you can set default headers for all requests.

### Request Bodies

A lot of networking libraries make request bodies a bit difficult. Bridge aims
to make them easy.

##### Plain Bodies

A description shouldn't be necessary for this:

```java
String postContent = "Hello, how are you?";
Request request = Bridge
    .post("https://someurl.com/post.js")
    .body(postContent)
	.request();
```

In addition to passing a `String`, other types of "plain" bodies include:

* byte[]
* JSONObject
* JSONArray 

There are other types of bodies discussed in the next few sections, along with 
in the [Request Conversion](https://github.com/afollestad/bridge#request-conversion)
section at the bottom (which is a bit more advanced).

##### Form Bodies

`Form`'s are commonly used with PUT/POST requests. They're basically the same 
thing as query strings with get requests, but the parameters are included in 
the body of the request rather than the URL.

```java
Form form = new Form()
    .add("Username", "Aidan")
    .add("Password", "Hello");
Request request = Bridge
    .post("https://someurl.com/login.js")
    .body(form)
	.request();
```

##### MultipartForm Bodies

A `MultipartForm` is a bit different than a regular form. Content is added as 
a "part" to the request body. The content is included as raw data associated with 
a content type, allowing you to include entire files. Multipart forms are commonly 
used in HTML forms (e.g. a contact form on a website), and they can be used for 
uploading files to a website.

```java
MultipartForm form = new MultipartForm()
    .add("Subject", "Hello")
    .add("Body", "Hey, how are you?")
    .add("FileUpload", new File("/sdcard/Download/ToUpload.txt"))
    .add("FileUpload2", "ToUpload2.mp4", Pipe.forFile(new File("/sdcard/Download/ToUpload2.mp4")));
Request request = Bridge
    .post("https://someurl.com/post.js")
    .body(form)
    .request();
```

This will automatically set the `Content-Type` header to `multipart/form-data`.

**Note**: `MultipartForm` has an `add()` method that accepts a `Pipe`. This can 
be used to add parts from streams (see the section below on how `Pipe` is used). 
`add()` for `File` objects is actually using this indirectly for you.

##### Streaming Bodies

Bridge's `Pipe` API allows you to easily stream data directly into a post body.

```java
Pipe pipe = new Pipe() {

    byte[] content = "Hello, this is a streaming example".getBytes();

    @Override
    public String hash() {
        // Creates a unique identifier based on the content being sent,
        // Used for duplicate request handling.
        return HashUtil.hash(content);
    }

    @Override
    public void writeTo(@NonNull OutputStream os, @Nullable ProgressCallback progressListener) throws IOException {
        os.write(content);
        // Notify optional progress listener that all data was transferred
        if (progressListener != null)
            progressListener.publishProgress(content.length, content.length);
    }

    @NonNull
    @Override
    public String contentType() {
        // text/plain since we are sending bytes that represent a string, or plain text.
        return "text/plain";
    }

    @Override
    public int contentLength() throws IOException {
        return content.length;
    }

    @Override
    public void close() {
        // Unused for this simple example, usually you would close streams and release resources
    }
};

Request request = Bridge
    .post("https://someurl.com/post.php")
    .body(pipe)
    .request();
```

**Note**: the value returned for `contentType()` in the `Pipe` is automatically set
to the value of the associated header. You can override that by changing the header 
after the body is set.

`Pipe` has three static convenience methods that create a pre-built `Pipe` instance
for certain uses:

```java
Pipe uriPipe = Pipe.forUri(this, Uri.parse(
    "content://com.example.provider/documents/images/1"));

Pipe filePipe = Pipe.forFile(new File("/sdcard/myfile.txt"));

InputStream is = // ...
Pipe transferPipe = Pipe.forStream(is, "text/plain", "unique-identifier-such-as-file-name");
```

They should be mostly self-explanatory.

### Info Callbacks

You can set an info callback to receive various events, including a 
connection being established, and request bodies being sent:

```java
Request request = Bridge
    .get("https://www.google.com")
    .infoCallback(new InfoCallback() {
        @Override
        public void onConnected(Request request) {
            // Connection to Google established
        }

        @Override
        public void onRequestSent(Request request) {
            // This method is optional to override
            // Indicates request body was sent to Google
        }
    }).request();
```

It's likely that more will be added to this later.

---

# Responses

Like requests, Bridge intends to make response interaction super easy.

### Response Basics

The code below should be mostly self explanatory:

```java
Request request = Bridge
	.get("https://www.google.com")
	.request();
	
Response response = request.response();
if (response.isSuccess()) {
	// Request returned HTTP status 200-300
} else {
	// Request returned an HTTP error status
}
```

You can also have Bridge throw an Exception in the event that `isSuccess()`
returns false:

```java
try {
	Request request  = Bridge
		.get("https://www.google.com")
		.throwIfNotSuccess()
		.request();
	Response response = request.response();
	// Use successful response
} catch(BridgeException e) {
	// See the error handling section
}
```

If you don't need a reference to the `Request` object, you can immediately retrieve the `Response`:

```java
Response response = Bridge
	.get("https://www.google.com")
	.response();
```

---

You can retrieve response headers similar to how request headers are set:

```java
Response response = // ...
String headerValue = response.header("Header-Name");
```

Headers can also have multiple values, separated by commas:

```java
Response response = // ...
List<String> values = response.headerList("Header-Name");
```

You can even retrieve the full map of headers:

```java
Response response = // ...
Map<String, List<String>> headers = response.headers();
```

Since *Content-Type* and *Content-Length* are commonly used response headers,
there's two convenience methods for these values:

```java
Response response = // ...
String contentType = response.contentType();
int contentLength = response.contentLength();
```

### Response Bodies

Bridge includes many methods to make converting responses to object types you need
easy. The code below should be self-explanatory:

```java
Response response = // ...

byte[] responseRawData = response.asBytes();

// Converts asBytes() to a UTF-8 encoded String.
String responseString = response.asString();

// If you set this to a TextView, it will display HTML formatting
Spanned responseHtml = response.asHtml();

// Cached in the Response object, using this method multiples will reference the same JSONObject.
// This allows your app to not re-parse the JSON if it's used multiple times.
JSONObject responseJsonObject = response.asJsonObject();

// Cached in the Response object, using this method multiples will reference the same JSONArray.
// This allows your app to not re-parse the JSON if it's used multiple times.
JSONArray responseJsonArray = response.asJsonArray();

// Don't forget to recycle!
// Once you use this method once, the resulting Bitmap is cached in the Response object,
// Meaning asBitmap() will always return the same Bitmap from any reference to this response.
Bitmap responseImage = response.asBitmap();

// Save the response content to a File of your choosing
response.asFile(new File("/sdcard/Download.extension"));

// Returns String, Spanned (for HTML), JSONObject, JSONArray, Bitmap, or byte[]
// based on the Content-Type header.
Object suggested = response.asSuggested();
```

If you're not interested in using the `Request` or `Response` object during
requests, you can immediately retrieve the response body:

```java
String responseBody = Bridge
	.get("https://www.google.com")
	.asString();
```

`asString()` could be replaced with any of the body conversion methods above.
Using this will automatically use `throwIfNotSuccessful()`, so a `BridgeException`
is thrown in case that the HTTP status code is not 200-300.

---

# Error Handling

The `BridgeException` class is used throughout the library and acts as a single 
exception provider. This helps avoid the need for different exception classes, 
or very unspecific Exceptions.

If you wanted, you could just display errors to the user using `BridgeException#getMessage()`. 
However, `BridgeException` lets you know exactly what happened before the user 
sees anything.

---

The `BridgeException#reason()` method returns a constants that indicate why the exception 
was thrown. If the Exception is for a request, you can retrieve the `Request` with 
`BridgeException#request()`. If the `Exception` is for a response, you can retrieve the 
`Response` with `BridgeException#response()`.

```java
BridgeException e = // ...
switch (e.reason()) {
    case BridgeException.REASON_REQUEST_CANCELLED: {
        Request request = e.request();
        // Used in BridgeExceptions passed to async request callbacks
        // when the associated request was cancelled.
        break;
    }
    case BridgeException.REASON_REQUEST_TIMEOUT: {
        Request request = e.request();
        // The request timed out (self explanatory obviously)
        break;
    }
    case BridgeException.REASON_REQUEST_FAILED: {
        Request request = e.request();
        // Thrown when a general networking error occurs during a request,
        // not including timeouts.
        break;
    }
    case BridgeException.REASON_RESPONSE_UNSUCCESSFUL: {
        Response response = e.response();
        // Thrown by throwIfNotSuccess(), when you explicitly want an
        // Exception be thrown if the status code was unsuccessful.
        break;
    }
    case BridgeException.REASON_RESPONSE_UNPARSEABLE: {
        Response response = e.response();
        // Thrown by the response conversion methods (e.g. asJsonObject())
        // When the response content can't be successfully returned in the
        // requested format. E.g. a JSON error.
        break;
    }
    case BridgeException.REASON_RESPONSE_IOERROR: {
        Response response = e.response();
        // Thrown by the asFile() response converter when the library
        // is unable to save the content to a file.
        break;
    }
    case BridgeException.REASON_RESPONSE_VALIDATOR_FALSE:
    case BridgeException.REASON_RESPONSE_VALIDATOR_ERROR:
        String validatorId = e.validatorId();
        // Discussed in the Validators section
        break;
}
```

**Note**: you do not need to handle all of these cases everywhere a `BridgeException` 
is thrown. The comments within the above example code indicate where those reasons are generally used.

---

# Async

Up until now, all code has been syncronous, meaning it gets run on the calling thread.
Android does not allow you to perform networking options on the UI thread, for good reasons.
Asyncronous requests become very important when you don't want to handle threading yourself.
Plus, Bridge comes with some huge advantages when using async methods.

### Async Requests

Here's a basic example of an async request. Obviously, `get()` can be replaced with the
other HTTP methods such as `post()`.

```java
Bridge
	.get("https://www.google.com")
	.throwIfNotSuccess() // optional
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            if (e != null) {
                // See the 'Error Handling' section for information on how to process BridgeExceptions
                int reason = e.reason();
            } else {
                // Use the Response object
				String responseContent = response.asString();
            }
        }
    });
```

Like syncronous requests, there are shortcuts to response conversion:

```java
Bridge
	.get("http://www.google.com")
    .asString(new ResponseConvertCallback<String>() {
        @Override
        public void onResponse(@NonNull Response response, @Nullable String object, @Nullable BridgeException e) {
            if (e != null) {
                // See the 'Error Handling' section for information on how to process BridgeExceptions
                int reason = e.reason();
            } else {
                // Use object parameter
            }
        }
    });
```

Not only is the calling thread *not* blocked, duplicate avoidance also comes 
into the picture (see the section below).

### Duplicate Avoidance

Duplicate avoidance is a feature of Bridge which allows you to avoid making multiple
requests to a URL at the same time.

```java
Bridge.get("http://www.google.com")
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use error or response
        }
    });
Bridge.get("http://www.google.com")
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use error or response
        }
    });
```

The above code requests Google's homepage in rapid succession. Since the first request
will most likely not finish before the computer has a chance to begin requesting the second,
Bridge will pool these requests together. The second request does not get exectued, instead, 
it waits for the first to finish executing, and returns the response to both callbacks at the same time.

There is no limit to how many requests can be pooled. Bridge's sample project requests an entire
page of images, but the image is only downloaded once.

**With POST/PUT requests, duplicate avoidance will also account for request bodies using MD5 hashes.
If request bodies are different, they will be considered two different requests that should both execute.**

### Upload Progress

Upload progress is pretty straight forward:

```java
Bridge
    .post("https://someurl.com/upload.js")
    .body(Pipe.forUri(this, data.getData()))
    .uploadProgress(new ProgressCallback() {
        @Override
        public void progress(Request request, int current, int total, int percent) {
            // Use progress
        }
    })
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use response
        }
    });
```

### Download Progress

The callback used to receive asyncronous request results has an optional `progress` method
that can be overidden in your callback:

```java
Bridge
    .get("http://someurl.com/bigfile.extension")
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use Response or error
        }

        @Override
        public void progress(Request request, int current, int total, int percent) {
            // Progress updates
        }
    });
```

**Note**: progress callbacks are only used if the library is able to deetermine the size 
of the content being downloaded. Generally, this means the requested URL needs to return a 
value for the *Content-Length* header. When it comes to `Pipe`'s, the `Pipe` handles reporting
progress to the progress callback on its own.

---

# Request Cancellation

Request cancellation is another cool feature that Bridge specializes in.
Note that it only works with asyncronous requests, syncronous requests
can't be cancelled since they can block the main thread.

### Cancelling Single Requests

The `Request` object has a simple `cancel()` method:

```java
Request request = Bridge
    .get("https://www.google.com")
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use error or response
        }
    });
request.cancel();
````

When the request is cancelled, the callback receives a `BridgeException`. `reason()` will
return `BridgeException.REASON_REQUEST_CANCELLED`. Most apps will probably ignore the error 
in this case.

### Cancelling Multiple Requests

The `Bridge` class allows you to cancel multiple (or all) active async requests.

##### All Active

This code will cancel all active requests, regardless of method or URL:

```java
Bridge.cancelAll().commit();
```

##### Method, URL/Regex

You can even cancel all active requests that match an HTTP method and a URL or regular expression pattern.

This will cancel all GET requests to any URL starting with http:// and ending with .png:

```java
int count = Bridge.cancelAll()
    .method(Method.GET)
    .url("http://.*\\.png")
    .commit();
```

`.*` is a wildcard in regular expressions, `\\` escapes the period to make it literal.

If you want to cancel all requests to a specific URL, you can use `Pattern.quote()` to specify a regex that matches literal text:

```java
int count = Bridge.cancelAll()
    .method(Method.GET)
    .url(Pattern.quote("http://www.android.com/media/android_vector.jpg"))
    .commit();
```

##### Tags

When making a request, you can tag it with a value (of any type):

```java
Bridge.get("http://www.google.com")
    .tag("Hello!")
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use response or error
        }
    });
```

You can then cancel all requests which have that tag:

```java
Bridge.cancelAll()
    .tag("Hello!")
    .commit();
```

### Preventing Cancellation

There are certain situations in which you wouldn't want to allow a request to be cancelled. 
For an example, your app may make calls to `Bridge.cancelAll().commit()` when an `Activity` pauses; 
that way, all requests that were active in that screen are cancelled. However, there may be a 
`Service` in your app that's making requests in the background that you would want to maintain. 
You can make those requests non-cancellable:

```java
Bridge.get("http://www.google.com")
    .cancellable(false)
    .request(new Callback() {
        @Override
        public void response(Request request, Response response, BridgeException e) {
            // Use response or error
        }
    });
```

This request will be ignored by `Bridge.cancelAll()` unless cancellation is forced:

```java
Bridge.cancelAll()
    .force()
    .commit();
```

---

# Validation

Validators allow you to provide consistent checking that certain conditions are true for a response.

```java
ResponseValidator validator = new ResponseValidator() {
    @Override
    public boolean validate(@NonNull Response response) throws Exception {
        JSONObject json = response.asJsonObject();
        return json.getBoolean("success");
    }

    @NonNull
    @Override
    public String id() {
        return "custom-validator";
    }
};
try {
    JSONObject response = Bridge
        .get("http://www.someurl.com/api/test")
        .validators(validator)
        .asJsonObject();
} catch (BridgeException e) {
    if (e.reason() == BridgeException.REASON_RESPONSE_VALIDATOR_FALSE) {
        String validatorId = e.validatorId();
        // Validator returned false
    } else if (e.reason() == BridgeException.REASON_RESPONSE_VALIDATOR_ERROR) {
        String validatorId = e.validatorId();
        String errorMessage = e.getMessage();
        // Validator threw an error
    }
}
```

The validator is passed before the request returns. Basically, the validator will check if a boolean 
field in the response JSON called *success* is equal to true. If you had an API on a server that returned 
true or false for this value, you could automatically check if it's true for every request with a single validator.

You can even use multiple validators for a single request:

```java
ResponseValidator validatorOne = // ...
ResponseValidator validatorTwo = // ...

try {
    JSONObject response = Bridge
        .get("http://www.someurl.com/api/test")
        .validators(validatorOne, validatorTwo)
        .asJsonObject();
} catch (BridgeException e) {
    if (e.reason() == BridgeException.REASON_RESPONSE_VALIDATOR_FALSE) {
        String validatorId = e.validatorId();
        // Validator returned false
    } else if (e.reason() == BridgeException.REASON_RESPONSE_VALIDATOR_ERROR) {
        String validatorId = e.validatorId();
        String errorMessage = e.getMessage();
        // Validator threw an error
    }
}
```

**Notes**: validators work great with async requests too! You can even apply validators
to every request in your application by setting global validators (discussed below).

---

# Configuration

Bridge allows you to set various parameters that are maintained
as long as your app stays in memory.

### Host Configuration

You can set a host that is used as the base URL for every request.

```java
Bridge.config()
    .host("http://www.google.com");
```

With Google's homepage set as the host, the code below would request `http://www.google.com/search?q=Hello`:

```java
Bridge
    .get("/search?q=%s", "Hello")
    .asString();
```
	
Basically, the URL you pass with each request is appended to the end of the host. If you were to pass a full URL 
(beginning with HTTP) in `get()` above, it would skip using the host for just that request.

### Default Headers

Default headers are headers that are automatically applied to every request. You don't have to do it yourself with every request in your app.

```java
Bridge.config()
    .defaultHeader("User-Agent", "Bridge Sample Code")
    .defaultHeader("Content-Type", "application/json")
    .defaultHeader("Via", "My App");
```

Every request, regardless of the method, will include those headers. You can override them at the individual request level by setting the header as you normally would.

### Timeout Configuration

You can configure how long the library will wait until timing out, either for connections or reading:

```java
Bridge.config()
    .connectTimeout(10000)
    .readTimeout(15000);
```

You can set timeouts at the request level too:

```java
Bridge.get("http://someurl.com/bigVideo.mp4")
    .connectTimeout(10000)
    .readTimeout(15000)
    .request();
```

### Buffer Size

The default buffer size is 1024 * 4 (4096). Basically, when you download a webpage or file, the buffer size is how big the byte array is with each pass. A large buffer size will create a larger byte array, which can affect memory usage, but it also increases the pace in which the content is downloaded.

The buffer size can easily be configured:

```java
Bridge.config()
    .bufferSize(1024 * 10);
```

Just remember to be careful with how much memory you consume, and test on various devices. 
You can set the buffer size at the request level too:

```java
Bridge.get("http://someurl.com/bigVideo.mp4")
    .bufferSize(1024 * 10)
    .response();
```

**Note**: the buffer size is used in a few other places, such as pre-built Pipe's (Pipe#forUri, Pipe#forStream, etc.).

### Logging

By default, logging is disabled. You can enable logging to see what the library is doing in your Logcat:

```java
Bridge.config()
    .logging(true);
```

### Global Validators

Validators for individual requests were shown above. You can apply validators to every request in your application:

```java
Bridge.config()
    .validators(new ResponseValidator() {
        @Override
        public boolean validate(@NonNull Response response) throws Exception {
            JSONObject json = response.asJsonObject();
            return json.getBoolean("success");
        }

        @NonNull
        @Override
        public String id() {
            return "custom-validator";
        }
    });
```

**Note**: you can pass multiple validators into the `validators()` method just like the individual request version.

---

# Cleanup

When you're done with Bridge (e.g. your app is terminating), you should call the `destroy()` method to avoid any memory leaks. Your app would be fine without this, but this is good practice and it helps speed up Java's garbage collection.

```java
Bridge.destroy();
```

**Note**: Calling this method will also cancel all active requests for you.

---

# Request Conversion

Bridge's request conversion feature allows you to use Java object instances directly as a request body. 
Objects are serialized directly into a format such as JSON.

### JSON Request Conversion

Take this class for an example:

```java
@ContentType("application/json")
public class Person {

    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Header(name = "Custom-Header")
    public String customHeader;

    @Body(name = "full_name")
    public String name;
    @Body
    public int age;
    @Body
    public Person spouse;   
}
```

The `ContentType` annotation is used to lookup what request converter should be used. The annotation's value
also gets applied as the `Content-Type` header of requests the object is passed in to.

The `Header` annotation can be used to apply request header values.

The `Body` annotations indicate fields that are serialized into the response body. The optional name
parameter can be used if the output name of the field should be different than the actual name of the field.

You can use instances of this class as a request body. Fields not marked with the `Header` or `Body`
annotation are ignored.

```java
Person person = new Person("Aidan Follestad", 21);
person.girlfriend = new Person("Waverly Moua", 19);
Request request = Bridge
    .post("https://someurl.com/post.js")
    .body(person)
    .request();
```

You can even send arrays or lists of this object as a request body (and it gets converted to a JSON Array):

```java
Person[] people = new People[] {
    new Person("Aidan Follestad", 21),
    new Person("Waverly Moua", 19)
};
Request request = Bridge
    .post("https://someurl.com/post.js")
    .body(people)
    .request();
```

### Dot Notation

I thought this was worthy of its own section. Bridge supports dot notation, which is better explained by example. 
Take this class:

```java
@ContentType("application/json")
public static class Person {

    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Body(name = "person.name")
    public String name = "Aidan";
    @Body(name = "person.age.$t")
    public int age = 21;
}
```

When converted to JSON, it will appear like this:

```json
{
    "person": {
        "name": "Aidan",
        "age": {
            "$t": 21
        }
    }
}
```

The dots in the names of the `Body` annotation parameters indicate a path of objects that it takes
to reach the value.

### Request Conversion API

Bridge comes stock with a JSON request converter, but the API is extensible for people like you.

See the [JsonRequestConverter](https://github.com/afollestad/bridge/blob/master/library/src/main/java/com/afollestad/bridge/conversion/JsonRequestConverter.java) 
source code for an example of how a `RequestConverter` is made. It should come off as simple.

When you have a custom converter made, you can register it to a Content-Type:

```java
Bridge.config()
    .requestConverter("application/json", new JsonRequestConverter());
```

---

# Response Conversion

Bridge's response conversion feature allows you convert responses directly to Java object instances. Objects
are de-serialized from a format such as jSON.

### JSON Response Conversion

Take this class for an example (notice that the `ContentType` annotation is not used with response 
conversion, but it should be left there if you plan on using request conversion):

```java
public class Person {

    public Person() {
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Header(name = "Custom-Header")
    public String customHeader;

    @Body(name = "name")
    public String name;
    @Body
    public int age;
    @Body
    public Person spouse;   
}
```

Imagine a URL that returns JSON like this:

```json
{
    "name": "Aidan Follestad",
    "age": 21,
    "spouse": {
        "name": "Waverly Moua",
        "age": 19
    }
}
```

You can retrieve that URL and convert the contents directly to a `Person` instance like this:

```java
Bridge.get("https://www.someurl.com/person.json")
    .asClass(Person.class, new ResponseConvertCallback<Person>() {
        @Override
        public void onResponse(@NonNull Response response, @Nullable Person object, @Nullable BridgeException e) {
            // Use response object
        }
    });
```

You can even retrieve arrays of JSON:

```json
[
    {
        "name": "Aidan Follestad",
        "age": 20,
        "spouse": {
            "name": "Waverly Moua",
            "age": 18
        }
    },
    {
        "name": "Waverly Moua",
        "age": 18
    }
]
```

```java
Bridge.get("https://www.someurl.com/person_array.json")
    .asClassArray(Person.class, new ResponseConvertCallback<Person[]>() {
        @Override
        public void onResponse(@NonNull Response response, @Nullable Person[] objects, @Nullable BridgeException e) {
            // Use response objects
        }
    });
```

### Dot Notation

See [Dot Notation](https://github.com/afollestad/bridge#dot-notation) under the Request Conversion API
 for details. It works the same way here, just in reverse.

### Response Conversion API

Bridge comes stock with a JSON response converter, but the API is extensible for people like you.

See the [JsonResponseConverter](https://github.com/afollestad/bridge/blob/master/library/src/main/java/com/afollestad/bridge/conversion/JsonResponseConverter.java) 
source code for an example of how a `ResponseConverter` is made. It should come off as simple.

When you have a custom converter made, you can register it to a Content-Type:

```java
Bridge.config()
    .responseConverter("application/json", new JsonResponseConverter());
```
