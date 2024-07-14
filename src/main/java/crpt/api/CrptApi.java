package crpt.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.semaphore = new Semaphore(requestLimit);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        long delay = timeUnit.toMillis(1);
        scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                delay, delay, TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature) throws InterruptedException, IOException {
        semaphore.acquire();
        try {
            String jsonRequest = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Failed to create document: " + response.body());
            }
        } finally {
            semaphore.release();
        }
    }

    public static class Document {
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;
        public Description description;

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }

        public static class Description {
            public String participantInn;
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);
        Document document = new Document();

        document.doc_id = "12345";
        document.doc_status = "NEW";
        document.doc_type = "LP_INTRODUCE_GOODS";
        document.importRequest = true;
        document.owner_inn = "7701234567";
        document.participant_inn = "7701234567";
        document.producer_inn = "7701234567";
        document.production_date = "2023-01-01";
        document.production_type = "TYPE";
        document.reg_date = "2023-01-01";
        document.reg_number = "12345";
        document.description = new Document.Description();
        document.description.participantInn = "7701234567";
        document.products = new Document.Product[1];
        document.products[0] = new Document.Product();
        document.products[0].certificate_document = "CERT_DOC";
        document.products[0].certificate_document_date = "2023-01-01";
        document.products[0].certificate_document_number = "12345";
        document.products[0].owner_inn = "7701234567";
        document.products[0].producer_inn = "7701234567";
        document.products[0].production_date = "2023-01-01";
        document.products[0].tnved_code = "CODE";
        document.products[0].uit_code = "UIT";
        document.products[0].uitu_code = "UITU";

        try {
            api.createDocument(document, "signature");
            System.out.println("Document created successfully");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}