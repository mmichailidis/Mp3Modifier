package gr.mich.mp3modifier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author KuroiTenshi
 */
public class Application {
    public static void main(String[] args) throws IOException, ParseException{        
        JSONParser parser = new JSONParser();
     
        Object obj = parser.parse(new FileReader("input.json"));
        JSONObject jsonObj = (JSONObject)obj;        
        
        JSONArray genres = (JSONArray)jsonObj.get("genre");  
        JSONArray files = (JSONArray)jsonObj.get("files");        
        JSONArray titleRemove = (JSONArray)jsonObj.get("titleRemove");
        JSONArray delim = (JSONArray)jsonObj.get("delim");
        Boolean isTitleRight = (Boolean) jsonObj.get("isTitleToTheRight");
        
        Iterator i = files.iterator();
        
        System.out.println("About to convert " + files.size() + " files");
        
        int j = 0;
        while(i.hasNext()) {
            j++;
            System.out.println("Converting " + j + " from " + files.size());
            JSONObject tmp = (JSONObject)i.next();
            String mp3Path = (String)tmp.get("mp3Path");
            String imagePath = (String)tmp.get("imagePath");
            changeFile(mp3Path, imagePath, titleRemove, genres, delim, isTitleRight);
        }     
    }
    
    private static void changeFile(String mp3Path, String imgPath, 
            JSONArray titleRemove, JSONArray genres, JSONArray delim,
            Boolean isTitleRight){
        
        File file = new File(mp3Path);
        File artwork = new File(imgPath);
                
        String title = file.getName();
        
        String genre = "";
        
        List<String> genreList = new ArrayList<>();
        List<String> titleDelete = new ArrayList<>();
        
        genres.stream().forEach((inner) -> {
            genreList.add(inner.toString());
        });
            
        titleRemove.stream().forEach((inner) -> {
            titleDelete.add(inner.toString());
        });
        
        for(String vLookUp:genreList){
            if(title.toLowerCase().contains(vLookUp.toLowerCase())) {
                genre = vLookUp;
                break;
            }
        }        
        
        String[] str = title.split("[.]");//remove the extension 
        title = str[0].trim();
        
        String tmpStr = title; //efficiently final for lambda 
        
        HashMap<Integer,String> tst = new HashMap<>();
        
        delim.stream().forEach((inner) -> {
            if(tmpStr.contains((String)inner)) {
                tst.put(tmpStr.indexOf((String)inner), (String)inner);                
            }
        });
        
        SortedSet<Integer> keys = new TreeSet<>(tst.keySet());
        
        for(Integer vLookUp:keys) {
            String tmp = tst.get(vLookUp);
            str = title.split("[" + tmp + "]");
            
            if (isTitleRight) {
                //true == title is right
                //the most right String that is left will be counted as the title
                title = str.length > 1 ? str[str.length - 1].trim() : str[0].trim();                
            } else {
                //the most left String will be counted as the title -> index 0
                title = str[0].trim();     
            }
        }
        
        for(String vLookUp:titleDelete){
            if (!title.toLowerCase().contains(vLookUp.toLowerCase())) {
                continue;
            }
            
            /*
            0 1 2 3 4 5 6 7 8
                2         7
            
            8 7 6 5 4 3 2 1 0
              1         6            
            */
            String tmp = new StringBuilder(title).reverse().toString();
            String fnd = new StringBuilder(vLookUp).reverse().toString();
            int i = tmp.toLowerCase().indexOf(fnd.toLowerCase());
            
            CharSequence subSequence = title.subSequence(
                    title.toLowerCase().indexOf(vLookUp.toLowerCase()),
                    title.length() - i
            );
            
            title = title.replace(subSequence, "");
        }
        
        AudioFile audio;
        
        try {
            audio = AudioFileIO.read(file);
            audio.setTag(new ID3v23Tag());
            
            Tag tag = audio.getTag();

            tag.setField(FieldKey.TITLE,title);
            tag.setField(FieldKey.GENRE,genre);

            Artwork cover = ArtworkFactory.createArtworkFromFile(artwork);
            tag.setField(cover);

            audio.commit();   
        } catch (CannotReadException | IOException | TagException 
                | ReadOnlyFileException | InvalidAudioFrameException 
                | CannotWriteException ex) {
            
            System.out.println("Failed to convert : " + file.getName());
        }     
    }
}
