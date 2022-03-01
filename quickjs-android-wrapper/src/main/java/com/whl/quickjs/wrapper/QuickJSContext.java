package com.whl.quickjs.wrapper;

import android.util.AndroidRuntimeException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class QuickJSContext {

    static {
        System.loadLibrary("quickjs-android-wrapper");
    }

    private static final String UNDEFINED = "undefined.js";


    public static QuickJSContext create() {
        return new QuickJSContext();
    }

    /**
     * 处理 Promise 等异步任务的消息循环队列
     */
    private static void executePendingJobLoop(QuickJSContext context) {
        int err;
        for(;;) {
            err = context.executePendingJob();
            if (err <= 0) {
                if (err < 0) {
                    throw new AndroidRuntimeException("Promise execute exception!");
                }
                break;
            }
        }
    }

    public interface ExceptionHandler {
        void handle(String error);
    }

    private final long context;
    private final NativeCleaner<JSObject> nativeCleaner = new NativeCleaner<JSObject>() {
        @Override
        public void onRemove(long pointer) {
            freeDupValue(context, pointer);
        }
    };
    private ExceptionHandler exceptionHandler;

    private QuickJSContext() {
        context = createContext();
    }

    public Object evaluate(String script) {
        return evaluate(script, UNDEFINED);
    }

    public Object evaluate(String script, String fileName) {
        Object obj = null;
        try {
            obj = evaluate(context, script, fileName);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        executePendingJobLoop(this);

        return obj;
    }

    public JSObject getGlobalObject() {
        return getGlobalObject(context);
    }

    public void destroyContext() {
        nativeCleaner.forceClean();
        destroyContext(context);
    }

    public String stringify(JSObject jsObj) {
        try {
            return stringify(context, jsObj.getPointer());
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }

    public Object getProperty(JSObject jsObj, String name) {
        try {
            return getProperty(context, jsObj.getPointer(), name);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        return null;
    }

    public void setProperty(JSObject jsObj, String name, Object value) {
        setProperty(context, jsObj.getPointer(), name, value);
    }

    public void freeValue(JSObject jsObj) {
        freeValue(context, jsObj.getPointer());
    }

    public void dupValue(JSObject jsObj) {
        dupValue(context, jsObj.getPointer());
    }

    public void freeDupValue(JSObject jsObj) {
        freeDupValue(context, jsObj.getPointer());
    }

    public int length(JSArray jsArray) {
        return length(context, jsArray.getPointer());
    }

    public Object get(JSArray jsArray, int index) {
        return get(context, jsArray.getPointer(), index);
    }

    Object call(JSObject func, long objPointer, Object... args) {
        Object obj = null;
        try {
            obj = call(context, func.getPointer(), objPointer, args);
        } catch (QuickJSException e) {
            if (exceptionHandler != null) {
                exceptionHandler.handle(writerToString(e));
            } else {
                e.printStackTrace();
            }
        }

        executePendingJobLoop(this);

        return obj;
    }

    /**
     * Automatically manage the release of objects，
     * the hold method is equivalent to call the
     * dupValue and freeDupValue methods with NativeCleaner.
     */
    public void hold(JSObject jsObj) {
        jsObj.dupValue();
        nativeCleaner.register(jsObj, jsObj.getPointer());
    }

    public JSObject createNewJSObject() {
        return parseJSON("{}");
    }

    public JSArray createNewJSArray() {
        return (JSArray) parseJSON("[]");
    }

    public JSObject parseJSON(String json) {
        return parseJSON(context, json);
    }

    public byte[] compile(String sourceCode) {
        return compile(context, sourceCode);
    }

    public Object execute(byte[] code) {
        return execute(context, code);
    }

    public Object evaluateModule(String script, String moduleName) {
        return evaluateModule(context, script, moduleName);
    }
    
    public Object evaluateModule(String script) {
        return evaluateModule(script, UNDEFINED);
    }

    public int executePendingJob() {
        return executePendingJob(context);
    }

    public void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    private String writerToString(QuickJSException e) {
        Writer writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    public void throwJSException(String error) {
        // throw $error;
        String errorScript = "throw " + "\"" + error + "\"" + ";";
        evaluate(errorScript);
    }

    // context
    private native long createContext();
    private native void destroyContext(long context);

    private native Object evaluate(long context, String script, String fileName) throws QuickJSException;
    private native Object evaluateModule(long context, String script, String fileName);
    private native JSObject getGlobalObject(long context);
    private native Object call(long context, long func, long thisObj, Object[] args) throws QuickJSException;

    private native Object getProperty(long context, long objValue, String name) throws QuickJSException;
    private native void setProperty(long context, long objValue, String name, Object value);
    private native String stringify(long context, long objValue) throws QuickJSException;
    private native int length(long context, long objValue);
    private native Object get(long context, long objValue, int index);
    private native void freeValue(long context, long objValue);
    private native void dupValue(long context, long objValue);
    private native void freeDupValue(long context, long objValue);

    // JSON.parse
    private native JSObject parseJSON(long context, String json);

    // bytecode
    private native byte[] compile(long context, String sourceCode);
    private native Object execute(long context, byte[] bytecode);

    // Promise
    private native int executePendingJob(long context);
}
