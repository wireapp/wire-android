import java.nio.file.*;
import org.codehaus.groovy.control.SourceUnit;
public final class ParseGroovy {
    public static void main(String[] args) throws Exception {
        var unit = SourceUnit.create(args[0], Files.readString(Path.of(args[0])));
        unit.parse();
        if (unit.getErrorCollector().hasErrors()) throw new AssertionError(unit.getErrorCollector().getErrors());
        System.out.println("Parsed Groovy syntax only: "+args[0]);
    }
}
