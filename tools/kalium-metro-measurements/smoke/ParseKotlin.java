import java.nio.file.*;
import org.jetbrains.kotlin.cli.jvm.compiler.*;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.*;
public final class ParseKotlin {
    public static void main(String[] args) throws Exception {
        var disposable = Disposer.newDisposable();
        try {
            var configuration = new CompilerConfiguration();
            try {
                var storage = Class.forName("org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar$ExtensionStorage");
                Class.forName("org.jetbrains.kotlin.cli.FrontendConfigurationKeysKt")
                    .getMethod("setExtensionsStorage", CompilerConfiguration.class, storage)
                    .invoke(null, configuration, storage.getConstructor().newInstance());
            } catch (ClassNotFoundException | NoSuchMethodException olderParser) { }
            var env = KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
            var factory = new KtPsiFactory(env.getProject(), false);
            for (String arg : args) {
                var file = factory.createFile(Path.of(arg).getFileName().toString(), Files.readString(Path.of(arg)));
                var errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class);
                if (!errors.isEmpty()) throw new AssertionError(arg+": "+errors);
                System.out.println("Parsed syntax only: "+arg);
            }
        } finally { Disposer.dispose(disposable); }
    }
}
