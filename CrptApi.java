import java.net.URI;

import java.net.http.HttpClient;

import java.net.http.HttpRequest;

import java.net.http.HttpResponse;

import java.util.concurrent.*;

import java.util.concurrent.locks.Lock;

import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.ObjectMapper;


public class CrptApi {

    private final Semaphore semaphore;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final Lock lock = new ReentrantLock();


    public CrptApi(TimeUnit timeUnit, int requestLimit) {

        if (requestLimit <= 0) {

            throw new IllegalArgumentException("requestLimit must be a positive number.");

        }

        this.semaphore = new Semaphore(requestLimit);

        this.httpClient = HttpClient.newHttpClient();

        this.objectMapper = new ObjectMapper();


        long interval = timeUnit.toMillis(1); // Convert timeUnit to milliseconds

        scheduler.scheduleAtFixedRate(this::resetRequestCount, interval, interval, TimeUnit.MILLISECONDS);

    }


    private void resetRequestCount() {

        lock.lock();

        try {
            semaphore.drainPermits();
            semaphore.release(requestLimit);

        } finally {

            lock.unlock();

        }

    }


    public void createDocument(Document document, String signature) throws InterruptedException, ExecutionException {

        semaphore.acquire();


        try {

            String json = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()

                    .uri(new URI("https://ismp.crpt.ru/api/v3/lk/documents/create"))

                    .header("Content-Type", "application/json")

                    .header("Authorization", "Bearer " + signature)

                    .POST(HttpRequest.BodyPublishers.ofString(json))

                    .build();


            CompletableFuture<HttpResponse<String>> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            HttpResponse<String> httpResponse = response.get();


            System.out.println("Response: " + httpResponse.body());

        } finally {

            semaphore.release();

        }

    }


    public static class Document {

        public Description description;

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


        // Add constructors, getters, and setters as needed


        public static class Description {

            public String participantInn;


            // Add constructors, getters, and setters as needed

        }


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


            // Add constructors, getters, and setters as needed

        }

    }


    public static void main(String[] args) throws InterruptedException, ExecutionException {

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);


        Document document = new Document();

        document.description = new Document.Description();

        document.description.participantInn = "1234567890";

        document.doc_id = "doc123";

        document.doc_status = "NEW";

        document.doc_type = "LP_INTRODUCE_GOODS";

        document.importRequest = true;

        document.owner_inn = "0987654321";

        document.participant_inn = "1122334455";

        document.producer_inn = "2233445566";

        document.production_date = "2020-01-23";

        document.production_type = "PRODUCT_TYPE";

        document.products = new Document.Product[]{

                new Document.Product() {

                    {

                        certificate_document = "cert123";

                        certificate_document_date = "2020-01-23";

                        certificate_document_number = "cert123456";

                        owner_inn = "0987654321";

                        producer_inn = "2233445566";

                        production_date = "2020-01-23";

                        tnved_code = "tnved123";

                        uit_code = "uit123";

                        uitu_code = "uitu123";

                    }

                }

        };

        document.reg_date = "2020-01-23";

        document.reg_number = "reg123";


        api.createDocument(document, "your_signature_here");

    }

} 