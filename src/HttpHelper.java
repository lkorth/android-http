import com.google.gson.Gson;

public class HttpHelper {

    private static Gson gson = null;

    public static Gson getGson() {
        if (gson == null)
            gson = new Gson();

        return gson;
    }

}
