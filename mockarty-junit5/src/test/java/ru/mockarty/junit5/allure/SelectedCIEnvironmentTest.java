package ru.mockarty.junit5.allure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SelectedCIEnvironmentTest{
 @TempDir Path dir;
 @Test void privateDeliveryNeverPersisted()throws Exception{
  Map<String,String> env=Map.of("MOCKARTY_CI_CONTEXT","private-envelope","alias","prefix-private-envelope","safe","kept");
  String text=Files.readString(AllureWriter.writeEnvironment(dir,env));assertFalse(text.contains("private-envelope"));assertFalse(text.contains("MOCKARTY_CI_CONTEXT"));assertTrue(text.contains("safe=kept"));assertEquals("prefix-private-envelope",env.get("alias"));
 }
}
