package nl.talsmasoftware.reflection;

import nl.talsmasoftware.reflection.beans.BeanReflectionSupport;

import java.io.Serializable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Een velden voor 'toString' implementaties die by default de klassenaam gebruikt met naam/waarde paren, gescheiden
 * door een {@code separator ", "} en voorzien van curly braces:
 * {@code "TypeNaam{veld=\"String waarde\", anderVeld=0, booleanVeld=false}"}
 *
 * @author <a href="mailto:info@talsmasoftware.nl">Sjoerd Talsma</a>
 */
public class ToStringBuilder implements Appendable, CharSequence, Serializable {
    private static final long serialVersionUID = 1L;

    protected final String prefix;
    protected final StringBuilder velden = new StringBuilder();

    // Aan te passen eigenschappen van deze builder:
    private String leftBracket = "{", separator = ", ", rightBracket = "}";
    private boolean includeNulls = false, forceBrackets = false;

    /**
     * Constructor om een ToStringBuilder voor een bepaald object of type aan te kunnen maken.
     *
     * @param source Het object waarvoor een ToStringBuilder gemaakt wordt (Object, Class, of een naam).
     */
    public ToStringBuilder(Object source) {
        this(null, source);
    }

    /**
     * Constructor om een nieuwe ToStringBuilder voor Object {@code source} op te zetten, waarbij alle instellingen van
     * een {@code parent} builder worden overgenomen indien aanwezig: brackets, separator, includeNulls en forceBrackets.
     *
     * @param parent De parent builder om settings van over te nemen (indien niet {@code null}).
     * @param source Het object om toString van te maken (of prefix string).
     */
    protected ToStringBuilder(ToStringBuilder parent, Object source) {
        if (source == null || source instanceof CharSequence) {
            this.prefix = nullToEmpty(source);
        } else {
            Class<?> type = source instanceof Class<?> ? (Class<?>) source : source.getClass();
            this.prefix = type.getSimpleName();
        }
        if (parent != null) {
            this.leftBracket = parent.leftBracket;
            this.separator = parent.separator;
            this.rightBracket = parent.rightBracket;
            this.includeNulls = parent.includeNulls;
            this.forceBrackets = parent.forceBrackets;
        }
    }

    /**
     * Deze methode geeft een {@link ToStringBuilder} terug waarbij alle properties uit het bron object gereflecteerd
     * worden.
     *
     * @param source Het bron object waarvoor een toString() implementatie op basis van reflectie van publieke velden
     *               moet plaatsvinden.
     * @return De toString builder waaraan alle publieke velden automatisch zijn toegevoegd.
     */
    public static ToStringBuilder reflect(Object source) {
        return source == null ? null
                : source instanceof CharSequence ? new ToStringBuilder(source)
                : new ToStringBuilder(source).appendReflectedPropertiesOf(source);
    }

    /**
     * Gebruikt reflectie om alle leesbare properties van de opgegeven bean aan de velden van deze builder toe te voegen.
     *
     * @param source Het object om de properties van te reflecteren en aan de velden van deze builder toe te voegen.
     * @return Referentie naar deze builder zelf voor method chaining toepassingen.
     */
    private ToStringBuilder appendReflectedPropertiesOf(Object source) {
        for (Map.Entry<String, Object> property : BeanReflectionSupport.getPropertyValues(source).entrySet()) {
            final String name = property.getKey();
            if (!"class".equals(name)) { // class zit al in de builder prefix.
                this.append(name, property.getValue());
            }
        }
        return this;
    }

    /**
     * Vertelt de velden dat {@code null} values moeten worden toegevoegd (by default {@code false}).
     *
     * @return Referentie naar deze builder met de bijgewerkte 'includeNulls' setting.
     */
    public ToStringBuilder includeNulls() {
        return includeNulls(true);
    }

    /**
     * Of {@code null} values moeten worden toegevoegd (by default {@code false}).
     *
     * @param includeNulls Of {@code null} values moeten worden toegevoegd.
     * @return Referentie naar deze builder met de bijgewerkte 'includeNulls' setting.
     */
    public ToStringBuilder includeNulls(boolean includeNulls) {
        this.includeNulls = includeNulls;
        return this;
    }

    /**
     * Stelt een andere veld separator in voor deze velden (by default {@code ", "}).
     * <p>
     * Let wel op dat deze separator niet geldt voor al toegevoegde waarden.
     *
     * @param separator De te gebruiken separator of {@code null} om helemaal geen separator te gebruiken.
     * @return Referentie naar deze builder met de bijgewerkte 'separator' setting.
     */
    public ToStringBuilder separator(CharSequence separator) {
        this.separator = nullToEmpty(separator);
        return this;
    }

    /**
     * Forceert het toevoegen van open en sluit haakjes, zelfs al zijn er helemaal geen velden aan deze builder
     * toegevoegd.
     * <p>
     * Het is tevens mogelijk om andere haakjes te specificeren zonder deze geforceerd toe te voegen:
     * {@link #brackets(String, String)}.
     *
     * @param openingBracket Het openingshaakje om te gebruiken voor dit object (standaard <code>"{"</code>).
     * @param closingBracket Het sluithaakje om te gebruiken voor dit object (standaard <code>"}"</code>).
     * @return De builder die altijd de open- en sluithaakjes toevoegt, zelfs al zijn er geen toegevoegde velden.
     * @see #brackets(String, String)
     */
    public ToStringBuilder forceBrackets(String openingBracket, String closingBracket) {
        this.forceBrackets = true;
        return brackets(openingBracket, closingBracket);
    }

    /**
     * Configureert de te gebruiken open en sluit haakjes.
     * <p>
     * Het is tevens mogelijk om andere haakjes te specificeren en deze tevens geforceerd toe te voegen, ook al zijn
     * er geen waardes opgenomen: {@link #forceBrackets(String, String)}.
     *
     * @param openingBracket Het openingshaakje om te gebruiken voor dit object (standaard <code>"{"</code>).
     * @param closingBracket Het sluithaakje om te gebruiken voor dit object (standaard <code>"}"</code>).
     * @return De builder met aangepaste open- en sluithaakjes.
     * @see #forceBrackets(String, String)
     */
    public ToStringBuilder brackets(String openingBracket, String closingBracket) {
        this.leftBracket = nullToEmpty(openingBracket);
        this.rightBracket = nullToEmpty(closingBracket);
        return this;
    }

    /**
     * Voegt het {@code toString()} resultaat van een superklasse toe aan deze builder. Dit werkt alleen goed als deze
     * superklasse dezelfde {@link #brackets(String, String)} gebruikt om diens velden tussen op te nemen.
     * <p>
     * Het is wellicht een betere overweging om de superklasse een ToStringBuilder te laten teruggeven die kan
     * worden overschreven om de eigen velden eenvoudig toe te voegen:<br><br>
     * <code>
     * protected ToStringBuilder toStringBuilder() {<br>
     * return super.toStringBuilder().append("veldnaam", "Veld waarde");<br>
     * }
     * </code>
     *
     * @param superToStringResult Het toString resultaat van de superklasse dat aan deze builder moet worden toegevoegd.
     * @return Deze builder met de velden uit de superklasse ook toegevoegd.
     */
    public ToStringBuilder appendSuper(CharSequence superToStringResult) {
        return append(new ToStringBetween(superToStringResult, leftBracket, rightBracket));
    }

    /**
     * De enige daadwerkelijke append implementatie van deze builder.
     * Alle andere append aanroepen dienen uiteindelijk hier terecht te komen.
     *
     * @param name  De naam van het veld dat wordt toegevoegd (optioneel).
     * @param value De waarde van het veld dat wordt toegevoegd.
     * @return Referentie naar deze builder met het veld toegevoegd.
     * @see #includeNulls()
     */
    public ToStringBuilder append(final CharSequence name, final Object value) {
        if (includeNulls || value != null) {
            if (velden.length() > 0) {
                velden.append(separator);
            }
            if (name != null && name.length() > 0) {
                velden.append(name).append('=');
            }
            this.appendValue(value);
        }
        return this;
    }

    /**
     * Recursie detector die appendValue van circulaire velden kan detecteren,
     * ongeacht de 'grootte' van de cirkel, dus hoeveel objecten er in het pad naar een al gerenderd object zitten.
     */
    private static final ThreadLocal<Map<Object, Object>> APPENDVALUE_RECURSION_DETECTOR = new ThreadLocal<Map<Object, Object>>() {
        @Override
        protected Map<Object, Object> initialValue() {
            return new IdentityHashMap<Object, Object>();
        }
    };

    /**
     * Converteert eventueel het te printen 'value' object naar een karakterreeks.
     * Deze methode wordt aangeroepen nadat al is bepaald dat value moet worden geprint,
     * dus wordt niet onnodig aangeroepen.
     * <p>
     * Er hoeft geen rekening gehouden te worden met separators of veldnamen; dat is allemaal v&oacute;&oacute;r
     * de aanroep van deze methode al afgehandeld door {@link #append(CharSequence, Object)}.
     * Het is dus normaal gesproken ook niet de bedoeling om zelf deze methode aan te roepen.
     * <p>
     * By default wordt hier simpelweg {@code "<null>"} toegevoegd voor {@code null} en een quoted string voor
     * alle {@link CharSequence} objecten, plus {@code toString()} van overige objecten.
     * <p>
     * Voor objecten waarbij de {@code toString()} de 'standaard' Object toString oplevert (dus met klassenaam en
     * hashcode), zal reflectie voor bean properties worden toegepast om toch een wat inhoudelijker representatie te
     * krijgen.
     *
     * @param value De toe te voegen waarde.
     */
    protected void appendValue(Object value) {
        final Map<Object, Object> sources = APPENDVALUE_RECURSION_DETECTOR.get();
        if (sources.containsKey(value)) { // recursie, ABSOLUUT NIET value.toString() aanroepen!
            velden.append(defaultToString(value));
        } else try {
            sources.put(value, value);
            _appendValue(value); // <<-- veilig om business methode aan te roepen.
        } finally {
            sources.remove(value);
        }
    }

    /**
     * De standaard 'implementatie' van {@link #appendValue(Object)} zoals we deze eigenlijk willen,
     * zonder rekening met recursie te hoeven houden.
     *
     * @param value De toe te voegen waarde.
     */
    private void _appendValue(Object value) {
        if (value == null) {
            velden.append("<null>");
        } else if (value instanceof CharSequence) {
            velden.append('"').append(value.toString().replaceAll("\"", "\\\"")).append('"');
        } else {
            final String stringValue = String.valueOf(value);
            if (equals(stringValue, defaultToString(value))) {
                velden.append(new ToStringBuilder(this, value).appendReflectedPropertiesOf(value));
            } else {
                velden.append(stringValue);
            }
        }
    }

    /**
     * Voegt de waarde van een veld toe aan deze builder, zonder naam voor het veld.
     *
     * @param value De waarde van het veld dat wordt toegevoegd.
     * @return Referentie naar deze builder met het veld toegevoegd.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(Object value) {
        return this.append(null, value);
    }

    /**
     * Voegt de waarde van een veld toe aan deze builder, zonder naam voor het veld.
     *
     * @param value De waarde van het veld dat wordt toegevoegd.
     * @return Referentie naar deze builder met het veld toegevoegd.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(CharSequence value) {
        return this.append((Object) value);
    }

    /**
     * Voegt een deel van de waarde van een veld toe aan deze builder, zonder naam voor het veld.
     *
     * @param csq   De sequence om deels toe te voegen.
     * @param start De start index van de toe te voegen sequence.
     * @param end   De eind index van de toe te voegen sequence.
     * @return Referentie naar deze builder met het veld toegevoegd.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(CharSequence csq, int start, int end) {
        return this.append(csq.subSequence(start, end));
    }

    /**
     * Voegt een enkel karakter als veldwaarde toe aan deze builder, puur omdat het kan in de {@link Appendable}.
     *
     * @param c De waarde van het karakter om als veldwaarde toe te voegen.
     * @return Referentie naar deze builder met het veld toegevoegd.
     * @see #append(CharSequence, Object)
     */
    public ToStringBuilder append(char c) {
        return this.append((Character) c);
    }

    /**
     * Maakt een subsequence van de actuele toestand van deze builder.
     *
     * @param start Het startkarakter.
     * @param end   Het eindkarakter.
     * @return De gevraagde subsequence van de huidige stand van deze builder.
     */
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    /**
     * Deze methode geeft aan of er 'content' moet worden gerenderd (tussen brackets). De standaard implementatie geeft
     * {@code forceBrackets || velden.length() > 0} terug.
     *
     * @return Of er content in deze {@code ToStringBuilder} tussen brackets moet worden gerenderd.
     */
    protected boolean hasContent() {
        return forceBrackets || velden.length() > 0;
    }

    /**
     * @return De actuele String representatie van deze builder.
     */
    @Override
    public String toString() {
        return hasContent()
                ? prefix + leftBracket + velden + rightBracket
                : prefix;
    }

    /**
     * @return De actuele lengte van deze builder.
     */
    public int length() {
        return hasContent()
                ? prefix.length() + leftBracket.length() + velden.length() + rightBracket.length()
                : prefix.length();
    }

    /**
     * Geeft een enkel karakter uit de builder terug.
     *
     * @param index De index van het gevraagde karakter.
     * @return Het karakter momenteel op positie {@code index} in deze builder.
     */
    public char charAt(int index) {
        if (index < prefix.length() || !hasContent()) {
            return prefix.charAt(index);
        } else {
            index -= prefix.length();
        }

        if (index < leftBracket.length()) {
            return leftBracket.charAt(index);
        } else {
            index -= leftBracket.length();
        }

        if (index < velden.length()) {
            return velden.charAt(index);
        } else {
            index -= velden.length();
        }

        return rightBracket.charAt(index);
    }

    /**
     * Geeft de 'standaard' String representatie zoals {@link Object#toString()} deze zou leveren.
     * Dit willen we weten om te bepalen wanneer we een appended 'value' middels reflection willen laten opnemen, of
     * wanneer dat niet nodig is (namelijk indien het Object over een eigen overschreven toString methode beschikt).
     *
     * @param value De waarde om de 'standaard' {@code Object#toString()} representatie van te bepalen.
     * @return De standaard toString representatie zoals de {@code Object} klasse die levert.
     */
    private static String defaultToString(Object value) {
        return value == null ? null : value.getClass().getName() + "@" + Integer.toHexString(value.hashCode());
    }

    private static String nullToEmpty(Object value) {
        return value != null ? value.toString() : "";
    }

    private static boolean equals(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }

    /**
     * Interne klasse om een toString() implementatie te leveren waarbij het resultaat tussen
     * eerste 'left' en laatste 'right' sequences wordt teruggegeven (indien gevonden) bij toString().
     */
    private static final class ToStringBetween {
        private final CharSequence source;
        private final String left, right;

        public ToStringBetween(CharSequence source, CharSequence left, CharSequence right) {
            this.source = source != null ? source : "";
            this.left = nullToEmpty(left);
            this.right = nullToEmpty(right);
        }

        /**
         * @return Het 'velden' deel van de super ToStringBuilder.
         */
        public String toString() {
            if (source instanceof ToStringBuilder) {
                return ((ToStringBuilder) source).velden.toString();
            }
            // Velden zoeken tussen eerste left en laatste right.
            String result = source.toString();
            final int leftIdx = result.indexOf(left);
            if (leftIdx >= 0) {
                final int start = leftIdx + left.length();
                final int end = result.lastIndexOf(right);
                result = start <= end ? result.substring(start, end) : result.substring(start);
            }
            return result;
        }
    }

}
