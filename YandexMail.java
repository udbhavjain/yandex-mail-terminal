import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.*;
import java.util.*;

/**
 * Created by udbhav on 2/4/17.
 */
public class YandexMail {

    private HashSet<String> singles = new HashSet<>();
    private LinkedHashMap<String,String> cookieMap = new LinkedHashMap<>();
    private ArrayList<JSONObject> messageList = new ArrayList<>();
    private static final String SENDER = "SENDER";
    private static final String DATE = "DATE";
    private static final String TITLE = "TITLE";
    private static final String LINK = "LINK";
    private static final String TYPE = "TYPE";
    private static final String THREAD = "thread";
    private static final String MSG = "message";
    private String currentPage;
    private HttpClient client;
    private static final String HOME = "https://mail.yandex.com/";









    public void setCookies(Header[] cookies)
    {

        for(Header cookie: cookies)
        {
            for(String field: cookie.getValue().toString().split(";"))
            {
                if(field.contains("="))
                {String[] tagval = field.split("=",2);
                cookieMap.put(tagval[0],tagval[1]);}
                else singles.add(field);
            }
        }
    }

    public String getCookie()
    {
        StringBuilder cookie = new StringBuilder();
        for(String cookietag: cookieMap.keySet())
        {
            cookie.append(cookietag + "=" + cookieMap.get(cookietag) + ";");
        }
        for(String cookietag: singles)
        {
            cookie.append(cookietag + ";");
        }
        cookie.deleteCharAt(cookie.length()-1);
        return cookie.toString();
    }



    public CloseableHttpResponse sendGetRequest(String url, HttpClient client) throws IOException
    {

            RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(10000).build();
            HttpGet get = new HttpGet(url);
            get.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36");
            get.setHeader("Accept", "text/html");
            get.setHeader("Accept-Encoding", "gzip, deflate, sdch, br");
            get.setHeader("Accept-Language", "en-GB,en-US;q=0.8,en;q=0.6");
            get.setHeader("Connection", "keep-alive");
            get.setHeader("Referer",currentPage);
            get.setConfig(config);
            if (cookieMap.size() != 0) get.setHeader("Cookie", getCookie());

           // System.out.println("\nSending GET request to : " + url);

            CloseableHttpResponse response = null;
            response = (CloseableHttpResponse) client.execute(get);
           // System.out.println(response.getStatusLine());
            currentPage = url;
            return response;


    }

    public CloseableHttpResponse sendPostRequest(String url, HttpClient client, BasicNameValuePair... pairs) throws IOException
    {


            HttpPost post = new HttpPost(url);
            try {
                post.setEntity(new UrlEncodedFormEntity(Arrays.asList(pairs)));
            }catch(UnsupportedEncodingException ux){};
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36");
            post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            post.setHeader("Accept-Encoding", "gzip, deflate, sdch, br");
            post.setHeader("Accept-Language", "en-GB,en-US;q=0.8,en;q=0.6");
            post.setHeader("Connection", "keep-alive");
            if (cookieMap.size() != 0) post.setHeader("Cookie", getCookie());

            //System.out.println("\nSending POST request to : " + url);

            CloseableHttpResponse response = (CloseableHttpResponse) client.execute(post);

            //System.out.println(response.getStatusLine());
            currentPage = url;

            return response;



    }

    public String downloadPage(CloseableHttpResponse response) throws IOException
    {

            InputStream instream = response.getEntity().getContent();

            byte[] buff = new byte[1024];
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int read = 0;

            instream = response.getEntity().getContent();

            while ((read = instream.read(buff)) != -1) {
                bout.write(buff, 0, read);
            }
            response.close();

        return new String(bout.toByteArray());




    }


    public void displayHome(String page)
    {
        Document doc = Jsoup.parse(page);
        doc.setBaseUri("https://mail.yandex.com/");

        Elements senders = doc.getElementsByClass("b-messages__from");
        Elements subjects = doc.getElementsByClass("b-messages__subject");
        Elements dates = doc.getElementsByClass("b-messages__date");

        boolean hasNext = false;
        boolean hasPrev = false;


        if(doc.getElementsByClass("b-pager__next").size()>0)
        {
            System.out.println("next");
            hasNext = true;
        }
        if(doc.getElementsByClass("b-pager__prev").size()>0)
        {
            System.out.println("previous");
            hasPrev = true;
        }


        messageList = new ArrayList<>();
        for (int i = 0; i < senders.size(); i++) {

            JSONObject mail = new JSONObject();

            mail.put(SENDER, senders.get(i).text());
            mail.put(DATE, dates.get(i).text());
            mail.put(TITLE, subjects.get(i).text());
            String link = senders.get(i).select("a").attr("abs:href");
            mail.put(LINK, link);
            if (link.contains("thread")) mail.put(TYPE, THREAD);
            else if (link.contains("message")) mail.put(TYPE, MSG);
            if (link.contains("new")) mail.put(TITLE, "[new]" + mail.get(TITLE));

            messageList.add(mail);


        }


        int num = 0;
        System.out.println(

                String.format("%2s%10s%30s\t\t%7s\t\t%5s","INDEX", SENDER,TITLE ,TYPE, DATE)
        );
        for(JSONObject object : messageList)
        {
            num++;
            System.out.println(

                    String.format("%2s%20s%30s\t%7s\t\t%5s",
                            num,
                            StringUtils.abbreviate(object.get(SENDER).toString(),18),
                            StringUtils.abbreviate(object.get(TITLE).toString(),28),
                            object.get(TYPE), object.get(DATE))


            );


        }

        boolean set;

        do {
            set =false;

            System.out.println("\nFunctions: \nopen \nrefresh \nlogout");
            if(hasNext)System.out.println("next");
            if(hasPrev)System.out.println("previous");
            if(!currentPage.matches(HOME))System.out.println("back");

            Scanner input = new Scanner(System.in);
            String cmd = input.nextLine();
            switch (cmd) {
                case ("open"): {

                        System.out.println("Enter index number: ");
                        int index;
                        boolean valid = true;
                        do {
                            while (!input.hasNextInt()) {
                                input.next();
                                System.out.print("Please enter an integer: ");
                            }
                            index = input.nextInt();
                            if(index > senders.size())
                            {
                                System.out.println("Inalid index!");
                                valid = false;
                            };
                        }while(!valid);


                        JSONObject mail = messageList.get(index - 1);

                        try {
                            CloseableHttpResponse response = sendGetRequest(mail.get(LINK).toString(), client);
                            setCookies(response.getHeaders("Set-Cookie"));
                            String content = downloadPage(response);
                            if (mail.get(TYPE) == THREAD) {

                                displayHome(content);
                            } else {
                                readMail(content);
                            }


                            set = false;
                        }catch(IOException iox){System.out.println("IO error!");}
                    break;


                }
                case("next"):
                {
                    if(hasNext) {
                        String nextpage = doc.getElementsByClass("b-pager__next").get(0).attr("abs:href");
                        try {
                            CloseableHttpResponse response = sendGetRequest(nextpage, client);
                            displayHome(downloadPage(response));
                            set = false;
                        }catch(IOException iox){System.out.println("IO error!");}
                    }
                    break;
                }
                case("previous"):
                {
                    if(hasPrev) {
                        String prevpage = doc.getElementsByClass("b-pager__prev").get(0).attr("abs:href");
                        try {
                            CloseableHttpResponse response = sendGetRequest(prevpage, client);
                            displayHome(downloadPage(response));
                            set = true;
                        }catch(IOException iox){System.out.println("IO error!");}
                    }
                    break;
                }

                case ("refresh"): {
                    refresh();
                    set = true;
                    break;
                }

                case("back"): {
                    goBack();
                    set = true;
                    break;
                }
                case("logout"):
                {
                    logout();
                    set = true;
                    break;
                }
                default: {
                    System.out.println("Invalid operation.");
                }
            }
        }while(!set);


    }


    public void readMail(String page)
    {

            Document doc = Jsoup.parse(page);
            doc.outputSettings().prettyPrint(false);
            Element subject = doc.getElementsByClass("b-message-head__subject-text").get(0);
            Element date = doc.getElementsByClass("b-message-head__date").get(0);
            Element receiver = doc.getElementsByClass("b-message-head__email").get(0);
            Element sender_mail= doc.getElementsByClass("b-message-head__email").get(1);
            Element sender_name = doc.getElementsByClass("b-message-head__person").get(0);
            Element content = doc.getElementsByClass("b-message-body__content").get(0);


            WordUtils util = new WordUtils();
            System.out.println( "\n\n"+
                    "Subject: " + subject.text() +"\n"
                    + "Date: " + date.text() +"\n"
                    + "To: " + receiver.text() + "\n"
                    + "Sender: " + sender_name.text() + " " + sender_mail.text() +"\n"
                    + "Content: \n" + util.wrap(content.text(),75)
            );
            System.out.println("\n\nFunctions: \nback");
            Scanner scanner = new Scanner(System.in);
            String cmd = scanner.nextLine();


            switch(cmd)
            {
                case("back"):
                {
                    goBack();
                    break;

                }
                case("logout"):
                {
                    logout();
                    break;
                }

            }


    }




    public void logout()
    {
        cookieMap.clear();
        System.exit(0);
    }


    public void goBack()
    {
        if(!currentPage.matches(HOME))
        {
            try {
                CloseableHttpResponse response = sendGetRequest(HOME, client);
                displayHome(downloadPage(response));
            }catch(IOException iox){System.out.println("IO Error!");}
        }

    }

    public void refresh()
    {
        try {
            CloseableHttpResponse response = sendGetRequest(currentPage, client);
            displayHome(downloadPage(response));
        }catch(IOException iox){System.out.println("IO error!");}
    };

    public void login()
    {
        try {

            Console reader = System.console();

            String uname = reader.readLine("Enter username: ");
            char[] pass = reader.readPassword("Enter password: ");


            client = HttpClientBuilder.create().disableCookieManagement().build();

            CloseableHttpResponse response;

            response = sendGetRequest("https://mail.yandex.com", client);
            setCookies(response.getHeaders("Set-Cookie"));
            response.close();

            response = sendPostRequest("https://passport.yandex.com/passport?mode=auth", client,
                    new BasicNameValuePair("login", uname), new BasicNameValuePair("passwd", new String(pass)));
            setCookies(response.getHeaders("Set-Cookie"));
            response.close();


            String loc = response.getFirstHeader("location").getValue().toString();
            response = sendGetRequest(loc, client);
            setCookies(response.getHeaders("Set-Cookie"));
            response.close();

            cookieMap.put("lite", "|483659962");
            cookieMap.put("_skin", "lite");

            response = sendGetRequest("https://mail.yandex.com/", client);
            setCookies(response.getHeaders("Set-Cookie"));

            displayHome(downloadPage(response));
        }catch(IOException iox)
        {
            System.out.println("IO error!");
            System.exit(1);
        }



    }
    public static void main(String[] args) {
        new YandexMail().login();
    }
}
