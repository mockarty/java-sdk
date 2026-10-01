package ru.mockarty.examples;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.CIJobContext;
import ru.mockarty.model.ExternalRunRequest;
/** Reports only the exact frozen selected item, then completes the job group. */
public final class SelectedCIExample {
 public static void main(String[] args)throws Exception{
  CIJobContext job=CIJobContext.load(System.getenv(CIJobContext.ENVIRONMENT));
  try(MockartyClient client=MockartyClient.builder().baseUrl(System.getenv("MOCKARTY_BASE_URL")).apiKey(System.getenv("MOCKARTY_API_TOKEN")).namespace(job.getNamespace()).build()){
   String status="passed";
   try{client.externalRuns().report(job.getNamespace(),new ExternalRunRequest().status(status).caseName("selected test").externalId(System.getenv("TEST_EXTERNAL_ID")).fullName(System.getenv("TEST_FULL_NAME")));}
   catch(Exception error){status="failed";}
   client.externalRuns().completeGroup(job.getNamespace(),job.callback(),status);
   if(!status.equals("passed"))System.exit(1);
  }
 }
}
