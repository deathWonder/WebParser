import org.apache.http.NameValuePair;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Request {
    private String method;
    private String path;
    private String scheme;
    private List<NameValuePair> queries = new LinkedList<>();
    private List<NameValuePair> bodies = new LinkedList<>();
    private List<String> headers;

    public String getMethod(){
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path){
        this.path = path;
    }
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
    public String getScheme() {
        return scheme;
    }
    public List<NameValuePair> getQueryParams() {
        return queries;
    }
    public void setQueries(List<NameValuePair> queries) {
        this.queries = queries;
    }
    public String getQueryParam(String name){
        for(NameValuePair pair: queries){
            if(name.equals(pair.getName())){
                return pair.getValue();
            }
        }
        return null;
    }
    public List<NameValuePair> getPostParams() {
        return bodies;
    }
    public void setBodies(List<NameValuePair> bodies) {
        this.bodies = bodies;
    }
    public String getPostParam(String name){
        for(NameValuePair pair: bodies){
            if(name.equals(pair.getName())){
                return pair.getValue();
            }
        }
        return null;
    }
    public List<String> getHeaders() {
        return headers;
    }
    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
    public Optional<String> getHead(String name){
        return extractHeader(headers, name);
    }
    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}