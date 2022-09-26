import java.util.ArrayList;

/**
 * Profanities: A Mary Whitehouse class which flags if the generated product key contains any of the listed profanities.
 *              These are all four-letter words as the product key is a set of four dash separated four character strings.
 */
public final class Profanities extends ArrayList<String> {
    Profanities()
    {
        this.add("CRAP");
        this.add("CR@P");
        this.add("SHIT");
        this.add("SH1T");
        this.add("FUCK");
        this.add("FVCK");
        this.add("CUNT");
        this.add("CVNT");
        this.add("PISS");
        this.add("P1SS");
        this.add("PI55");
        this.add("P155");
        this.add("PI5S");
        this.add("P15S");
        this.add("PIS5");
        this.add("P1S5");
    }

    /**
     * containsAny: Returns true if the generated product ket contains any of the listed profanities
     * @param key: The generated product key to test for profanities.
     * @return: true if any profanities appear within the string, else false.
     */
    public boolean containsAny(String key)
    {
        boolean retVal = false;
        for(String prof: this)
        {
            if(key.contains(prof))
            {
                retVal = true;
                break;
            }
        }
        return retVal;
    }
}
