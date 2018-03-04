package br.com.munif.framework.vicente.domain.experimental;

import br.com.munif.framework.vicente.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VicSerializer {

    private static final String IDENT_TRUE = "    ";
    private static final String LN_TRUE = "\n";
    private static final String IDENT_FALSE = "";
    private static final String LN_FALSE = "";
    private static String IDENT = IDENT_FALSE;
    private static String LN = LN_FALSE;
    public static String QUOTE = "\"";

    public static void setIdentention(boolean b) {
        if (b) {
            IDENT = IDENT_TRUE;
            LN = LN_TRUE;
        } else {
            IDENT = IDENT_FALSE;
            LN = LN_FALSE;
        }
    }

    private static final VicSerializer INSTANCE = new VicSerializer();

    public Map<Object, String> visitados = new HashMap<>();

    public static VicSerializer getInstance() {
        return INSTANCE;
    }

    private VicSerializer() {
    }

    public String serialize(Object object) {
        visitados = new HashMap<>();
        return serialize(object, IDENT);
    }

    public String serialize(Object object, String ident) {
        String ln = LN + ident;
        if (object == null) {
            return null;
        }

        try {
            Class clazz = object.getClass();
            if (object instanceof BaseEntity) {
                visitados.put(object, "{" + QUOTE + "class" + QUOTE + ":" + QUOTE + "" + clazz.getCanonicalName() + QUOTE + "," + QUOTE + "id" + QUOTE + ":" + QUOTE + "" + ((BaseEntity) object).getId() + "" + QUOTE + "," + QUOTE + "version" + QUOTE + ":" + QUOTE + "" + ((BaseEntity) object).getVersion() + "" + QUOTE + "}");
            }
            if (clazz.isArray()) {
                StringBuilder toReturn = new StringBuilder();
                toReturn.append("[");
                for (int i = 0; i < Array.getLength(object); i++) {
                    toReturn.append((i != 0 ? "," : "") + ln + serialize(Array.get(object, i), ident + IDENT));
                }
                toReturn.append(ln.substring(0, ln.length() - IDENT.length()) + "]");
                return toReturn.toString();
            }
            String packageName = clazz.getPackage().getName();
            if (packageName.startsWith("java.lang")) {
                return QUOTE + object + QUOTE;
            }
            StringBuilder toReturn = new StringBuilder();
            toReturn.append("{" + ln + "" + QUOTE + "class" + QUOTE + ":" + QUOTE + "" + clazz.getCanonicalName() + QUOTE);
            int lugar = toReturn.length();

            for (Class c = clazz; !Object.class.equals(c); c = c.getSuperclass()) {
                StringBuilder sbf = new StringBuilder();
                Field[] fields = c.getDeclaredFields();
                for (Field f : fields) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0) {
                        continue;
                    }
                    if (f.isAnnotationPresent(JsonIgnore.class)) {
                        continue;
                    }
                    f.setAccessible(true);
                    Object value = f.get(object);
                    String sValue = "";
                    if (visitados.containsKey(value)) {
                        if (value instanceof BaseEntity) {
                            //System.out.println("----> HIT " + ((BaseEntity) value).getClassName() + " " + ((BaseEntity) value).getId());
                        }
                        sValue = visitados.get(value);
                    } else {
                        sValue = serialize(value, ident + IDENT);
                        visitados.put(value, sValue);
                    }
                    sbf.append("," + ln + QUOTE + f.getName() + QUOTE + ":" + sValue);

                }
                toReturn.insert(lugar, sbf);
            }

            toReturn.append(ln.substring(0, ln.length() - IDENT.length()) + "}");

            return toReturn.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ex.toString();
        }
    }

}
