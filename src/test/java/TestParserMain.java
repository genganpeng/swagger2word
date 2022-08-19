import org.word.model.Table;
import org.word.parser.SwaggerParserContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestParserMain {
    private static String jsonStr = null;

    static {
        Path path = Paths.get("D:\\swagger-api.json");
        byte[] data = new byte[0];
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jsonStr = new String(data);
    }


    public static void main(String[] args) throws Exception {
        List<Table> result = new ArrayList<>();
        SwaggerParserContext swaggerParserContext = new SwaggerParserContext(jsonStr);
        Map<String, Object> map = swaggerParserContext.doParse(result);
        System.out.println(map.toString());
    }
}
