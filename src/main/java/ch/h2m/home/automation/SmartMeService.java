package ch.h2m.home.automation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class SmartMeService {

    public static Observable<BigDecimal> smartMeObservable(String deviceName) {

        return Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                .map(tick -> callSmartMe())
                .doOnError(err -> System.err.println("Error retrieving messages"))
                .retry()
                .map(jsonAsString -> Converter.getJsonArray(jsonAsString))
                .map(jsonArray -> jsonArray.toArray())
                .flatMap(Observable::fromArray)
                .map(obj -> (JsonObject) obj)
                .filter(jsonObject -> deviceName.equalsIgnoreCase(jsonObject.getString("Name")))
                .filter(jsonObject -> jsonObject.containsKey("Temperature"))
                .map(jsonObject -> jsonObject.getJsonNumber("Temperature"))
                .map(jsonNumber -> jsonNumber.bigDecimalValue().setScale(0, RoundingMode.HALF_UP))
                .distinct(); //Hashcode has to be implemented!
    }

    private static String callSmartMe() {
        Client client = ClientBuilder.newClient();
        String authorization = PropertyStore.getInstance().getValue("smartme.authorization");
        String uri = PropertyStore.getInstance().getValue("smartme.uri");
        Response response = client
                .target(uri)
                .path("Devices")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .header("cache-control", "no-cache")
                .header("authorization", authorization)
                .get();

        return response.readEntity(String.class);
    }
}
