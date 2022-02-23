package software.wings.delegatetasks.utils;

public class UrlUtil {

    public static String appendPathToBaseUrl(String baseUrl, String path) {
        if (baseUrl.charAt(baseUrl.length() - 1) != '/') {
            baseUrl += '/';
        }
        if (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return baseUrl + path;
    }

}
