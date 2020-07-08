package mt.edu.um.malteseaacapp.operations;

import java.text.Normalizer;

public class TextOps {

    /**
     * Returns a normalised version of an un-normalised string
     * @param unNormalisedWord  The un-normalised string
     * @return Returns the normalised version of the string
     */
    public static String normalise (String unNormalisedWord)
    {
        String normalisedWord = Normalizer.normalize(unNormalisedWord, Normalizer.Form.NFD);
        // Since ħ and Ħ don't get normalised by by the 'normalize' method
        normalisedWord = normalisedWord.replaceAll("ħ", "h");
        normalisedWord = normalisedWord.replaceAll("Ħ", "H");
        normalisedWord = normalisedWord.replaceAll("[^\\p{ASCII}]", "");
        return normalisedWord;
    }
}
