package gr.mich.mp3modifier;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    public static void main(String[] args) throws IOException, TagException,
            ReadOnlyFileException, InvalidAudioFrameException, 
            CannotReadException, CannotWriteException, ParseException{
        
        JSONParser parser = new JSONParser();
     
        Object obj = parser.parse(new FileReader("input.json"));
        JSONObject jsonObj = (JSONObject)obj;        
        
        JSONArray genres = (JSONArray)jsonObj.get("genre");  
        JSONArray files = (JSONArray)jsonObj.get("files");        
        JSONArray titleRemove = (JSONArray)jsonObj.get("titleRemove");
        
        Iterator i = files.iterator();
        
        System.out.println("About to convert " + files.size() + " files");
        
        int j = 0;
        while(i.hasNext()) {
            j++;
            System.out.println("Converting " + j + " from " + files.size());
            JSONObject tmp = (JSONObject)i.next();
            String mp3Path = (String)tmp.get("mp3Path");
            String imagePath = (String)tmp.get("imagePath");
            changeFile(mp3Path, imagePath, titleRemove, genres);
        }     
    }
    
    private static void changeFile(String mp3Path, String imgPath, JSONArray titleRemove, JSONArray genres)
            throws CannotReadException, IOException, TagException, 
            ReadOnlyFileException, InvalidAudioFrameException, 
            CannotWriteException {
        
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
        
        title = title.replace(".mp3", "").trim();
        
        for(String vLookUp:genreList) {
            title = title.replaceAll("(?i)" + vLookUp,"");
        }
        
        //not working rly good.. w/e
        for(String vLookUp:titleDelete) {
            title = title.replaceAll("(?i)" + vLookUp,"");
        }
        
        title = title.trim().replace("-", "").trim();
        
        AudioFile audio = AudioFileIO.read(file);
                
        audio.setTag(new ID3v23Tag());
        
        Tag tag = audio.getTag();
        
        tag.setField(FieldKey.TITLE,title);
        tag.setField(FieldKey.GENRE,genre);
        
        Artwork cover = ArtworkFactory.createArtworkFromFile(artwork);
        tag.setField(cover);
        
        audio.commit();       
    }
}
