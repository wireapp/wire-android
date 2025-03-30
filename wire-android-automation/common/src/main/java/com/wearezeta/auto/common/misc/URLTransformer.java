package com.wearezeta.auto.common.misc;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class URLTransformer {

    public static String addQueryParameter(String url, String parameter) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }
        String newQuery = uri.getQuery();
        if (newQuery == null) {
            newQuery = parameter;
        } else {
            newQuery += "&" + parameter;
        }
        try {
            URI newURI = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
            return newURI.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure while transforming URL", e);
        }
    }

    public static String removeQueryParameter(String url, String parameter) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }
        String query = uri.getQuery();
        String newQuery = "";
        if (query.contains("&")) {
            newQuery = Arrays.stream(query.split("&"))
                    .filter(p -> !p.contains(parameter + "="))
                    .collect(Collectors.joining("&"));
        } else {
            if (!query.contains(parameter + "=")) {
                newQuery = query;
            }
        }
        try {
            URI newURI = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), newQuery, uri.getFragment());
            return newURI.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure while transforming URL", e);
        }
    }

    public static String getQueryParameter(String url, String parameterName) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }
        String query = uri.getQuery();
        if (query.contains("&")) {
            for (String param : query.split("&")) {
                if (param.startsWith(parameterName)) {
                    return param.substring(parameterName.length() + 1);
                }
            }
        }
        return null;
    }

    public static String changePath(String url, String path) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }

        try {
            URI newURI = new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
            return newURI.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure while transforming URL", e);
        }
    }

    public static String changePathAndQuery(String url, String pathAndQuery) {
        String path = pathAndQuery;
        if (pathAndQuery.contains("?")) {
            String parameter = pathAndQuery.substring(pathAndQuery.indexOf("?") + 1);
            if (parameter.contains("#")) {
                String fragment = parameter.substring(parameter.indexOf("#") + 1);
                url = URLTransformer.changeFragment(url, fragment);
                parameter = parameter.substring(0, parameter.indexOf("#"));
            }
            url = URLTransformer.addQueryParameter(url, parameter);
            path = pathAndQuery.substring(0, pathAndQuery.indexOf("?") - 1);
        }
        return URLTransformer.changePath(url, path);
    }

    public static String changeFragment(String url, String fragment) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }

        try {
            URI newURI = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), fragment);
            return newURI.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure while transforming URL", e);
        }
    }

    public static String changeHost(String url, String host) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }

        try {
            URI newURI = new URI(uri.getScheme(), host, uri.getPath(), uri.getQuery(), uri.getFragment());
            return newURI.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failure while transforming URL", e);
        }
    }

    public static String getHost(String url) {
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }
    }

    public static String getPath(String url) {
        try {
            return new URI(url).getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }
    }

    public static String getQuery(String url) {
        try {
            return new URI(url).getQuery();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Input URL is wrong:" + url, e);
        }
    }
}
