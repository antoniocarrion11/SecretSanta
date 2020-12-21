import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SecretSanta {

    private static final String WELCOME_MESSAGE = "WELCOME TO SECRET SANTA CREATOR \n " +
            "will you be using a file or typing in your participants? \n";
    private static final String ENTER_SIZE = "Enter the size of your party \n";
    private static final String ENTER_EMAILS = "The size of your party is: %2d, go ahead and enter their info... \n";
    private static final String FIRST_THEIR_EMAIL = "First their email: ";
    private static final String THEN_THEIR_NAME = "Then their name: ";
    private static final String FINALLY_THEIR_ADDY = "Finally, give me their address";
    private static final String HERE_ARE_YOUR_PARTICIPANTS = "Great! Here are your list of participants and their assigned receiver... \n";
    private static final String RESTART_PROMPT = "Is this configuration satisfactory? Y/N";
    private static final String CONFIRMATION = "Alright I'll send out the email!";

    private List<SecretSantaParticipant> santaParty = new ArrayList<>();
    private LinkedHashMap<SecretSantaParticipant, SecretSantaParticipant> partyWithRecipients = new LinkedHashMap<>();
    private int sizeOfParty = 0;
    private boolean restart = true;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LinkedHashMap<SecretSantaParticipant, SecretSantaParticipant> findRecepients() {

        try {
            //Prompt user for data using BufferReader
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            //loop for user input will restart if user requests it
            while (restart) {
                restart = false;

                //Prompt user for input
                System.out.println(WELCOME_MESSAGE);
                String answer = reader.readLine();

                if("file".equalsIgnoreCase(answer)){
                    //Logic for finding recipients by file
                    fileParticipants(reader);
                } else if("type".equalsIgnoreCase(answer) || "typing".equalsIgnoreCase(answer)){
                    //Logic for finding recipients by typing
                    typeParticipants(reader);
                } else {
                    System.out.println("Please tell me if you want to use a file or type!!!");
                    continue;
                }

                //Restart prompt
                System.out.println(RESTART_PROMPT);
                answer = reader.readLine();
                if (answer.equalsIgnoreCase("YES") || answer.equalsIgnoreCase("y")) {
                    System.out.println(CONFIRMATION);
                } else {
                    restart = true;
                }
            }
            reader.close();
        } catch (IOException | URISyntaxException e){
            e.printStackTrace();
            System.out.println("There was a problem with your input so sorry!");
        }
        return partyWithRecipients;
    }

    public void typeParticipants(BufferedReader reader) throws IOException {
        System.out.println(ENTER_SIZE);
        sizeOfParty = Integer.parseInt(reader.readLine());


        //Generate a range of numbers to sizeOfParty and shuffle the range
        List<Integer> range = derangement();

        //Enter the emails of each of the participants
        System.out.println(String.format(ENTER_EMAILS, sizeOfParty));
        for (int i = 0; i < sizeOfParty; i++) {
            System.out.print(FIRST_THEIR_EMAIL);
            String emailRecipient = reader.readLine();
            System.out.print(THEN_THEIR_NAME);
            String name = reader.readLine();
            System.out.println(FINALLY_THEIR_ADDY);
            String address = reader.readLine();
            santaParty.add(new SecretSantaParticipant(emailRecipient, name, address));
        }

        checkParticipants(range);
    }

    public void fileParticipants(BufferedReader reader) throws IOException, URISyntaxException {
        System.out.println("Enter the file name...");
        String filename = reader.readLine();

        ClassLoader classLoader = getClass().getClassLoader();
        File resource = new File(classLoader.getResource(filename).getFile());

        //File is found
        System.out.println("File Found : " + resource.exists());

        //Read File Content
        String content = new String(Files.readAllBytes(resource.toPath()));
        System.out.println(content);

        //Read file and serialize to list
        santaParty = Arrays.asList(objectMapper.readValue(content, SecretSantaParticipant[].class));

        if(!santaParty.isEmpty()){
            sizeOfParty = santaParty.size();
        }

        //Generate a range of numbers to sizeOfParty and shuffle the range
        List<Integer> range = derangement();

        checkParticipants(range);
    }

    private void checkParticipants(List<Integer> range){
        //Reprint for confirmation and restart or send the email
        System.out.println(HERE_ARE_YOUR_PARTICIPANTS);
        for (int i = 0; i < sizeOfParty; i++) {
            System.out.print(i + "'s giving their gift to: ");
            System.out.println(range.get(i));
            partyWithRecipients.put(santaParty.get(i), santaParty.get(range.get(i)));
        }
    }

    private List<Integer> derangement(){
        Random rand = new Random();
        List<Integer> temp = new ArrayList<>();
        List<Integer> range = IntStream.range(0, sizeOfParty).boxed().collect(Collectors.toList());
        for(int i = 0; i < sizeOfParty; i++){
            int j = rand.nextInt(sizeOfParty);
            if (!range.get(i).equals(j) && i != range.get(j)){
                temp.add(range.get(i));
                range.set(i, range.get(j));
                range.set(j, temp.get(0));
                temp.remove(0);
            } else {
                i--;
            }
        }
        return range;
    }
}