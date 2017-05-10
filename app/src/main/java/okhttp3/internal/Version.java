package okhttp3.internal;

public class Version {
    public static String userAgent() {
        return "okhttp/${project.version}";
    }

    private Version() {
    }
}
