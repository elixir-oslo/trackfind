package no.uio.ifi.trackfind.backend.scripting.python;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.uio.ifi.trackfind.backend.scripting.AbstractScriptingEngine;
import org.python.core.*;
import org.python.util.PythonInterpreter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Python implementation of the Scripting Engine.
 *
 * @author Dmytro Titov
 */
@Component
public class PythonScriptingEngine extends AbstractScriptingEngine {

    private PythonInterpreter commonInterpreter;

    private LoadingCache<String, PyCode> scripts = CacheBuilder.newBuilder()
            .maximumSize(100)
            .build(
                    new CacheLoader<String, PyCode>() {
                        public PyCode load(String script) {
                            return commonInterpreter.compile(script);
                        }
                    });

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLanguage() {
        return "Python";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object executeInternally(String script, Map dataset) throws Exception {
        PythonInterpreter localInterpreter = new PythonInterpreter();
        localInterpreter.set(properties.getScriptingDatasetVariableName(), dataset);
        localInterpreter.eval(scripts.get(script));
        return convertToJava(localInterpreter.get(properties.getScriptingResultVariableName()));
    }

    protected Object convertToJava(Object result) {
        if (result == null || result instanceof PyNone) {
            return null;
        }

        if (!(result instanceof PyObject)) {
            return result;
        }

        PyObject pythonObject = (PyObject) result;

        String qualifiedClassName = pythonObject.getType().getModule() + "." + pythonObject.getType().getName();
        try {
            return pythonObject.__tojava__(Class.forName(qualifiedClassName));
        } catch (ClassNotFoundException ignored) {
        }

        if (pythonObject instanceof PyArray) {
            return Arrays.stream((Object[]) ((PyArray) pythonObject).getArray()).map(this::convertToJava).collect(Collectors.toList());
        }

        if (pythonObject instanceof PyString) {
            return pythonObject.toString();
        }

        return pythonObject.__tojava__(Object.class);
    }

    @Autowired
    public void setCommonInterpreter(PythonInterpreter commonInterpreter) {
        this.commonInterpreter = commonInterpreter;
    }

}
