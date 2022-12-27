package com.xingin.xhs;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.arm.backend.Unicorn2Factory;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.*;
import com.github.unidbg.linux.android.dvm.array.ArrayObject;
import com.github.unidbg.linux.android.dvm.array.ByteArray;
import com.github.unidbg.linux.android.dvm.jni.ProxyDvmObject;
import com.github.unidbg.memory.Memory;
import okhttp3.internal.connection.RealCall;
import okhttp3.internal.http.CallServerInterceptor;
import okhttp3.internal.http.RealInterceptorChain;
import okio.BufferedSink;
import okhttp3.*;
import okio.Buffer;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;

public class Sign extends AbstractJni {

    private final String deviceId;
    private final String mainHmac;
    private final VM vm;
    private final DvmClass i;
    private final long t;
    private final AndroidEmulator emulator;
    private final String platformInfo;

    private Sign(String deviceId, String mainHmac) {
        this.deviceId = deviceId;
        this.mainHmac = mainHmac;
        platformInfo = "platform=android&build=7673009&deviceId=" + deviceId;
        emulator = AndroidEmulatorBuilder
                .for64Bit()
                .setProcessName("com.xingin.xhs")
                .addBackendFactory(new Unicorn2Factory(true))
                .build();
        emulator.getSyscallHandler().setEnableThreadDispatcher(true);
        emulator.getBackend().registerEmuCountHook(100000);
        emulator.getSyscallHandler().setVerbose(true);
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));
        memory.setCallInitFunction(true);
        vm = emulator.createDalvikVM(new File("unidbg-android/src/test/resources/xhs/xiaohongshu7673.apk"));
        vm.setJni(this);
        vm.setVerbose(true);

//        DalvikModule dm = vm.loadLibrary("tiny", true);
//        dm.callJNI_OnLoad(emulator);
//        DvmClass tiny = vm.resolveClass("com/xingin/tiny/internal/t");
//        String data = "{\"appid\":\"ECFAAF01\",\"channel\":\"KS-XINGKONG-CPA\",\"did\":\"\",\"ext\":\"1\",\"magic\":1908582296,\"model\":\"ONEPLUS A6010\",\"os\":\"android\",\"os_preview_version\":0,\"os_version\":29,\"sdk_version\":\"1.8.8\"}";
//        ArrayObject arrayObject = new ArrayObject(
//                ProxyDvmObject.createObject(vm,"POST"),
//                ProxyDvmObject.createObject(vm,"https://as.xiaohongshu.com/api/v1/cfg/android"),
//                ProxyDvmObject.createObject(vm, new byte[]{})
//        );
//        Object obj = tiny.callStaticJniMethodObject(emulator, "a(I[Ljava/lang/Object;)Ljava/lang/Object;",
//                -1819964227, arrayObject);
//        System.out.println(obj);
        i = vm.resolveClass("com/xingin/shield/http/XhsHttpInterceptor");
        DalvikModule dm = vm.loadLibrary("xyass", true);
        dm.callJNI_OnLoad(emulator);
        i.callStaticJniMethod(emulator, "initializeNative()V");
        t = i.newObject(null).callJniMethodLong(emulator, "initialize(Ljava/lang/String;)J", "main");
    }

    @Override
    public DvmObject<?> getStaticObjectField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "com/xingin/shield/http/ContextHolder->sLogger:Lcom/xingin/shield/http/ShieldLogger;":
                return vm.resolveClass("com/xingin/shield/http/ShieldLogger").newObject(null);
            case "com/xingin/shield/http/ContextHolder->sDeviceId:Ljava/lang/String;":
                return new StringObject(vm, deviceId);
        }
        return super.getStaticObjectField(vm, dvmClass, signature);
    }

    @Override
    public int getStaticIntField(BaseVM vm, DvmClass dvmClass, String signature) {
        switch (signature) {
            case "com/xingin/shield/http/ContextHolder->sAppId:I":
                return 0xecfaaf01;
        }
        return super.getStaticIntField(vm, dvmClass, signature);
    }

    @Override
    public void callVoidMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "com/xingin/shield/http/ShieldLogger->nativeInitializeStart()V":
            case "com/xingin/shield/http/ShieldLogger->nativeInitializeEnd()V":
            case "com/xingin/shield/http/ShieldLogger->initializeStart()V":
            case "com/xingin/shield/http/ShieldLogger->initializedEnd()V":
            case "com/xingin/shield/http/ShieldLogger->buildSourceStart()V":
            case "com/xingin/shield/http/ShieldLogger->buildSourceEnd()V":
            case "com/xingin/shield/http/ShieldLogger->calculateStart()V":
            case "com/xingin/shield/http/ShieldLogger->calculateEnd()V":
                return;
            case "okhttp3/RequestBody->writeTo(Lokio/BufferedSink;)V": {
                BufferedSink bufferedSink = (BufferedSink) vaList.getObjectArg(0).getValue();
                RequestBody requestBody = (RequestBody) dvmObject.getValue();
                if (requestBody != null) {
                    try {
                        requestBody.writeTo(bufferedSink);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return;
            }
        }
        super.callVoidMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callObjectMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "android/content/Context->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;": {
                return vm.resolveClass("android/content/SharedPreferences").newObject(vaList.getObjectArg(0));
            }
            case "android/content/SharedPreferences->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;": {
                if (((StringObject) dvmObject.getValue()).getValue().equals("s")) {
                    if (vaList.getObjectArg(0).getValue().equals("main")) {
                        return new StringObject(vm, "");
                    }
                    if (vaList.getObjectArg(0).getValue().equals("main_hmac")) {
                        return new StringObject(vm, mainHmac);
                    }
                }
            }
            case "okhttp3/Interceptor$Chain->request()Lokhttp3/Request;": {
                RealInterceptorChain chain = (RealInterceptorChain) dvmObject.getValue();
                return vm.resolveClass("okhttp3/Request").newObject(chain.request());
            }
            case "okhttp3/Request->url()Lokhttp3/HttpUrl;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/HttpUrl").newObject(request.url());
            }
            case "okhttp3/HttpUrl->encodedPath()Ljava/lang/String;": {
                HttpUrl httpUrl = (HttpUrl) dvmObject.getValue();
                return new StringObject(vm, httpUrl.encodedPath());
            }
            case "okhttp3/HttpUrl->encodedQuery()Ljava/lang/String;": {
                HttpUrl httpUrl = (HttpUrl) dvmObject.getValue();
                return new StringObject(vm, httpUrl.encodedQuery());
            }
            case "okhttp3/Request->body()Lokhttp3/RequestBody;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/RequestBody").newObject(request.body());
            }
            case "okhttp3/Request->headers()Lokhttp3/Headers;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/Headers").newObject(request.headers());
            }
            case "okio/Buffer->writeString(Ljava/lang/String;Ljava/nio/charset/Charset;)Lokio/Buffer;": {
                Buffer buffer = (Buffer) dvmObject.getValue();
                StringObject s = vaList.getObjectArg(0);
                String string = s.getValue();
                Charset charset = (Charset) vaList.getObjectArg(1).getValue();
                buffer.writeString(string, charset);
                return vm.resolveClass("okio/Buffer").newObject(buffer);
            }
            case "okhttp3/Headers->name(I)Ljava/lang/String;": {
                Headers headers = (Headers) dvmObject.getValue();
                String name = headers.name(vaList.getIntArg(0));
                return new StringObject(vm, name);
            }
            case "okhttp3/Headers->value(I)Ljava/lang/String;": {
                Headers headers = (Headers) dvmObject.getValue();
                String name = headers.value(vaList.getIntArg(0));
                return new StringObject(vm, name);
            }
            case "okio/Buffer->clone()Lokio/Buffer;": {
                Buffer buffer = (Buffer) dvmObject.getValue();
                return vm.resolveClass("okio/Buffer").newObject(buffer.clone());
            }
            case "okhttp3/Request->newBuilder()Lokhttp3/Request$Builder;": {
                Request request = (Request) dvmObject.getValue();
                return vm.resolveClass("okhttp3/Request$Builder").newObject(request.newBuilder());
            }
            case "okhttp3/Request$Builder->header(Ljava/lang/String;Ljava/lang/String;)Lokhttp3/Request$Builder;": {
                Request.Builder builder = (Request.Builder) dvmObject.getValue();
                StringObject name = vaList.getObjectArg(0);
                StringObject value = vaList.getObjectArg(1);
                builder.header(name.getValue(), value.getValue());
                return vm.resolveClass("okhttp3/Request$Builder").newObject(builder);
            }
            case "okhttp3/Request$Builder->build()Lokhttp3/Request;": {
                Request.Builder builder = (Request.Builder) dvmObject.getValue();
                Request request = builder.build();
                return vm.resolveClass("okhttp3/Request").newObject(request);
            }
            case "okhttp3/Interceptor$Chain->proceed(Lokhttp3/Request;)Lokhttp3/Response;": {
                Request request = (Request) vaList.getObjectArg(0).getValue();
                Response response = new Response(request, Protocol.HTTP_1_1, "", 200, null,
                        request.headers(), null, null, null, null, 0, 0, null);
                return vm.resolveClass("okhttp3/Response").newObject(response);
            }
        }
        return super.callObjectMethodV(vm, dvmObject, signature, vaList);
    }

    @Override
    public DvmObject<?> callStaticObjectMethodV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "java/nio/charset/Charset->defaultCharset()Ljava/nio/charset/Charset;":
                return vm.resolveClass("java/nio/charset/Charset").newObject(Charset.defaultCharset());
            case "com/xingin/shield/http/Base64Helper->decode(Ljava/lang/String;)[B":
                String input = (String) vaList.getObjectArg(0).getValue();
                byte[] result = Base64.getDecoder().decode(input);
                return new ByteArray(vm, result);
        }
        return super.callStaticObjectMethodV(vm, dvmClass, signature, vaList);
    }

    @Override
    public DvmObject<?> newObjectV(BaseVM vm, DvmClass dvmClass, String signature, VaList vaList) {
        switch (signature) {
            case "okio/Buffer-><init>()V":
                return dvmClass.newObject(new Buffer());
        }
        return super.newObjectV(vm, dvmClass, signature, vaList);
    }

    @Override
    public int callIntMethodV(BaseVM vm, DvmObject<?> dvmObject, String signature, VaList vaList) {
        switch (signature) {
            case "okhttp3/Headers->size()I": {
                Headers headers = (Headers) dvmObject.getValue();
                return headers.size();
            }
            case "okio/Buffer->read([B)I": {
                Buffer buffer = (Buffer) dvmObject.getValue();
                byte[] sink = (byte[]) vaList.getObjectArg(0).getValue();
                return buffer.read(sink);
            }
            case "okhttp3/Response->code()I": {
                return 200;
            }
        }
        return super.callIntMethodV(vm, dvmObject, signature, vaList);
    }

    public String getShield(String url, Map<String, String> headers) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
        }
        Request request = builder.build();
        RealInterceptorChain c = new RealInterceptorChain(
                new RealCall(new OkHttpClient(), request, false),
                new ArrayList<CallServerInterceptor>(), 0, null, request, 0, 0, 0);
        DvmObject<?> chain = this.vm.resolveClass("okhttp3/Interceptor$Chain").newObject(c);
        DvmObject<?> resp = i.callStaticJniMethodObject(emulator, "intercept(Lokhttp3/Interceptor$Chain;J)Lokhttp3/Response;", chain, t);
        Response response = (Response) resp.getValue();
        return response.header("shield");
    }

    public String getPlatformInfo() {
        return this.platformInfo;
    }

    public String getVideoSearchUrl(String keyword) {
        try {
            return "http://edith.xiaohongshu.com/api/sns/v10/search/notes?keyword=" +
                    URLEncoder.encode(keyword, "UTF-8") +
                    "&filters=%5B%7B%22tags%22%3A%5B%22%E8%A7%86%E9%A2%91%E7%AC%94%E8%AE%B0%22%5D%2C%22type%22%3A%22filter_note_type%22%7D%5D" +
                    "&sort=&page=1&page_size=20&source=explore_feed" +
                    "&session_id=2b0te6rtzi9nz2ymcefwg" +
                    "&api_extra=&page_pos=0&pin_note_id=&allow_rewrite=1" +
                    "&geo=&loaded_ad=&query_extra_info=&rec_extra_params=" +
                    "&preview_ad=&scene=&is_optimize=0&location_permission=0" +
                    "&device_level=4";

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public Map<String, String> getBeforeInterceptHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-b3-traceid", "50504ddeae1b0600");
        headers.put("x-legacy-smid", "20221216155456b727bda37b2ecbb1cedaf88c0e6cab990105b5b9da900a4e");
        headers.put("x-legacy-did", "8fa2bc4d-c133-39b7-9cf1-d64f2b8e6b3a");
        headers.put("x-legacy-fid", "167118471610a2c0b1785df9fe318b9fb87a05272cfe");
        headers.put("x-legacy-sid", "session.1671185082040052454936");
        headers.put("x-mini-gid", "7d3e46c010df550a3a603cdf7d03be52f12a2e8947359e0f778367a6");
        headers.put("x-mini-sig", "b3c6b12a25e4e07cd445db9fbb0f9d880fba3613a2fa81f5de347ea5df4438fc");
        headers.put("x-mini-mua", "eyJhIjoiRUNGQUHGMDEiLCJjIjoxMDIsImsiOiI2OGUzODEwNDAwNzhhNzA2YjQwZDg2MzM5YmE0YTJlODMyMWFjNDg4OWNhN2I2M2U3NDJkYjQ5MTdiYjI5ZTIxIiwicCI6ImEiLCJzIjoiZDRkZTU2ODQ0NmFiMDAyODFjNTQxOTFmMzQ0NTIzNmMiLCJ0Ijp7ImMiOjExOSwiZCI6NiwicyI6NDA5OCwidCI6MjA0OTk5OTEsInR0IjpbMV19LCJ1IjoiMDAwMDAwMDA0M2RmNjdlMDA2ZmY4ODk4OGQxMTcxZWRiMjg3ZjMzZCIsInYiOiIxLjguOCJ9.pXbhyEx54mXYH82ONBQv1w_9Io_4VeiCMYkIoSPD72E4nTq1zWU7vOh3QEgp6yHv9T5z7ZgS9rO50fBWL2hrpenFEHPJvS2bDYc_wrLsDztzp2UJagukfuiGKNkRKqWNxj-GoqN1ECkVC8x6sRg99IRhDaDZEZMWMyaU0_srM8-DYL-mTetVQQIEwjgX190RTadD9xMd70bg-wYkR_fmEA-iNBVfAb9mJFn5ufo3m470BnfEBoF5VAKIlcFc5iE9EBnnzVfqk32Y55m6QFHb2kVxxGtHPIysP-8lnczmwVfgjSgSdmpXQ3hORT0IFloR76CGCmKegpG6dkZbg1CYgjiS2PRsEgTmwvi5cPKDMEHE2hD8NZRNdRdn7SdHgkCadEOd3HG5o_c8IHBWtjJpcZ-HHUHIm-BFTROhRgSPcZqMcUmrQ4sxPD6pAwiupSNK9mOM9mezwMhLilNOA08LZJ3fEQG7jU0E-YzgOPhit-HHg9_iDMQjslsCkoNQn4cT1-3aNGElERoJoU0Wybl5H2Po37gNbKgzyrFZ7lVUtPtzGTzEBSODSrFfI0ozG5CkX447v2Fic99dgcYtKg-tnZ2U3z1QGm97F_cwSmooWUxA8t71FYCx1_6GL343rB6QTMXz4m1AM4PYjRkZTtM_apSFTZHU0tRTGgPSAcigNDgPiIgsECvcEIHuTY4iAdz32oV6XhxYXDEmjA6-zDBj96MyBO9SWvj0rA3vQebnHyVQReghPMzH2Dr4C-SzLrs5oAWpOh_uwjNWO8PXLWzNPRjALjCs2m_khWYVB2StNBRvBQ0fnewC_bWKYYzo-4jj8deIjNCjPDiK06JjWuWjPqEEYidab13uWPcKtBwdwOt3U4wthTw0s8bmEGhHiOmLsd9_6M4qIEDZMa0nGZCBklxPeg1EwT6Xhow2ayEkNqU.");
        headers.put("xy-common-params", "fid=167118472610a2c0b1785df9fe318b9fb87a05272cfe&device_fingerprint=20221216155556b727bda37b2ecbb1cedaf88c0e6cab990105b5b9da900a4e&device_fingerprint1=20221216155556b727bda37b2ecbb1cedaf88c0e6cab990105b5b9da900a4e&cpu_name=&gid=7d3e46c010df550b3a603cdf7d03be52f12a2e8947359e0f778367a6&device_model=phone&launch_id=1671184726&tz=Asia%2FShanghai&channel=KS-XINGKONG-CPA&versionName=7.67.3.1&deviceId=8fa2bc4d-c123-39b7-9cf1-d64f2b8e6b3a&platform=android&sid=session.1671185082040052454936&identifier_flag=4&t=1671185144&project_id=ECFAAF&build=7673009&x_trace_page_current=search_result_notes&lang=zh-Hans&app_id=ECFAAF01&uis=light&teenager=0");
        headers.put("user-agent", "Dalvik/2.1.0 (Linux; U; Android 10; ONEPLUS A6010 Build/QKQ1.190716.003) Resolution/1080*2340 Version/7.67.3.1 Build/7673009 Device/(OnePlus;ONEPLUS A6010) discover/7.67.3.1 NetType/WiFi");
        headers.put("referer", "https://app.xhs.cn/");
        headers.put("accept-encoding", "gzip");
        return headers;
    }

    public static String doGet(String url, Map<String, String> headers) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String resultString = "";
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpHost proxy = new HttpHost("127.0.0.1", 8089, "http");
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(2000).setConnectTimeout(2000).setProxy(proxy).build();
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(requestConfig);
            for (Map.Entry<String, String> s: headers.entrySet()) {
                httpGet.setHeader(s.getKey(), s.getValue());
            }
            response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultString;
    }

    public static void main(String[] args) {
        String deviceId = "8fa2bc4d-c123-39b7-9cf1-d62f2b8e6b3a";
        String mainHmac = "5Opy/47vBg/Xma7/sd7I9f9yjJKRYbzFM7v+EUost7QAcDBNgrDCWFJTjo/0gr8kKWFrXSu11wo1daWQZewTOvVGiBZBNeOvVYPPT3xn2Av6gapdi4fgmS/KQRceXKjb";
        Sign sign = new Sign(deviceId, mainHmac);
        String url = sign.getVideoSearchUrl("china");
        Map<String, String> headers = sign.getBeforeInterceptHeaders();
        String shield = sign.getShield(url, headers);
        System.out.println(shield);
        String platformInfo = sign.getPlatformInfo();
        System.out.println(platformInfo);
        headers.put("shield", shield);
        headers.put("xy-platform-info", platformInfo);
        String result = doGet(url, headers);
        System.out.println(url);
        System.out.println(headers);
        System.out.println(result);
    }
}
