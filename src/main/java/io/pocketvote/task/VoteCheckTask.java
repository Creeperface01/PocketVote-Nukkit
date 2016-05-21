package io.pocketvote.task;

import cn.nukkit.Server;
import cn.nukkit.scheduler.AsyncTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.pocketvote.PocketVote;
import io.pocketvote.event.VoteEvent;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;

public class VoteCheckTask extends AsyncTask {

    private PocketVote plugin;
    private String identity;
    private String secret;
    private String version;

    public VoteCheckTask(PocketVote plugin, String identity, String secret, String version) {
        this.plugin = plugin;
        this.identity = identity;
        this.secret = secret;
        this.version = version;
    }

    @Override
    public void onRun() {
        plugin.getLogger().debug("Checking for outstanding votes.");
        String url = "https://api.pocketvote.io/check";

        try {

            URL obj = new URL(url);
            HttpURLConnection con;
            con = (HttpsURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "PocketVote Nukkit v" + version);
            con.setRequestProperty("Identity", identity);

            //int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            in.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.toString());

            if(json.get("code").asText().equalsIgnoreCase("success") && json.has("payload")) {
                try {
                    Jws<Claims> claims = Jwts.parser().setSigningKey(secret.getBytes("UTF-8")).parseClaimsJws(json.get("payload").asText());
                    setResult(claims.getBody());
                } catch(SignatureException e) {
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onCompletion(Server server) {
        if(hasResult()) {
            if(getResult() instanceof DefaultClaims) {
                DefaultClaims claims = (DefaultClaims) getResult();
                claims.values().stream().filter(o -> o instanceof LinkedHashMap).forEach(o -> server.getPluginManager().callEvent(new VoteEvent(((LinkedHashMap) o).get("player").toString(), ((LinkedHashMap) o).get("ip").toString(), ((LinkedHashMap) o).get("site").toString())));
            }
        }
    }

}