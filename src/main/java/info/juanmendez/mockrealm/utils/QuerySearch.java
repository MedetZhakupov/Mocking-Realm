package info.juanmendez.mockrealm.utils;

import org.mockito.internal.util.reflection.Whitebox;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import info.juanmendez.mockrealm.decorators.RealmListDecorator;
import info.juanmendez.mockrealm.dependencies.Compare;
import info.juanmendez.mockrealm.models.Query;
import io.realm.Case;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.exceptions.RealmException;

/**
 * Created by Juan Mendez on 3/7/2017.
 * www.juanmendez.info
 * contact@juanmendez.info
 *
 * This util does a deep search through and check if the given haystack
 * has any realmModel which meets the condition.
 */
public class QuerySearch {

    Query query;
    ArrayList<String> types;
    Object needle;
    ArrayList<Object> needles;

    Class clazz;
    Case casing = Case.SENSITIVE;

    Object inLeft;
    Object inRight;

    public RealmList<RealmModel> search(Query query, RealmList<RealmModel> haystack ) {

        this.query = query;
        String condition = query.getCondition();
        Object[] arguments = query.getArgs();

        this.types = new ArrayList<>(Arrays.asList(((String) arguments[0]).split("\\.")));

        int argsLen = arguments.length;

        if( argsLen >= 2){
            this.needle = arguments[1];
            this.clazz = RealmModelUtil.getClass(needle);

            if ((clazz == String.class || clazz == String[].class) && argsLen >= 3) {
                casing = (Case) arguments[2];
            }
        }

        if (condition == Compare.between) {
            this.inLeft = arguments[1];
            this.inRight = arguments[2];
        }
        else if (condition == Compare.in) {
            needles = new ArrayList<>(Arrays.asList((Object[]) needle));
        }

        if (casing == Case.INSENSITIVE) {

            if (condition == Compare.in) {

                for (int i = 0; i < needles.size(); i++) {
                    needles.set(i, ((String) needles.get(i)).toLowerCase());
                }
            } else {
                this.needle = ((String) needle).toLowerCase();
            }
        }

        RealmList<RealmModel> queriedList = RealmListDecorator.create();
        Boolean returnValue;
        for (RealmModel realmModel : haystack) {
            returnValue = checkRealmObject(realmModel, 0);

            if( !query.getAsTrue() )
                returnValue = !returnValue;

            if (returnValue) {
                queriedList.add(realmModel);
            }
        }

        return queriedList;
    }

    private boolean checkRealmObject(RealmModel realmModel, int level) {
        //RunTimeErrorException if search field is not found in realmQueryClass

        Object value;
        String condition = query.getCondition();

        try {
            value = Whitebox.getInternalState(realmModel, types.get(level));
        } catch (Exception e) {
            throw (new RealmException( "'" + RealmModelUtil.getClass(realmModel).getName() + "' doesn't have the attribute '" + types.get(level) + "'"));
        }

        if (value != null) {

            if (level < types.size() - 1) {

                if (value instanceof RealmList) {
                    RealmList<RealmModel> valueList = (RealmList<RealmModel>) value;

                    for (RealmModel rm : valueList) {
                        //at least one item must meet the requirements!
                        if (checkRealmObject(rm, level + 1)) {
                            return true;
                        }
                    }

                    return false;
                } else if (value instanceof RealmModel) {
                    return checkRealmObject((RealmModel) value, level + 1);
                }

                throw (new RealmException(types.get(level) + " is of neither type RealmList, or RealmModel"));
            }

            if (condition == Compare.equal) {

                if (clazz == Date.class && (((Date) value)).compareTo((Date) needle) == 0) {
                    return true;
                } else if (casing == Case.INSENSITIVE) {
                    return needle.equals(((String) value).toLowerCase());
                } else if (needle.equals(value)) {
                    return true;
                }
            } else if (condition == Compare.less) {

                if (clazz == Date.class && (((Date) value)).compareTo((Date) needle) < 0) {
                    return true;
                } else if (clazz == Byte.class && ((byte) value) < ((byte) needle)) {
                    return true;
                } else if (clazz == Integer.class && ((int) value) < ((int) needle)) {
                    return true;
                } else if (clazz == Double.class && ((double) value) < ((double) needle)) {
                    return true;
                } else if (clazz == Long.class && ((long) value) < ((long) needle)) {
                    return true;
                } else if (clazz == Float.class && ((float) value) < ((float) needle)) {
                    return true;
                } else if (clazz == Short.class && ((short) value) < ((short) needle)) {
                    return true;
                }
            } else if (condition == Compare.lessOrEqual) {

                if (clazz == Date.class && (((Date) value)).compareTo((Date) needle) <= 0) {
                    return true;
                } else if (clazz == Byte.class && ((byte) value) <= ((byte) needle)) {
                    return true;
                } else if (clazz == Integer.class && ((int) value) <= ((int) needle)) {
                    return true;
                } else if (clazz == Double.class && ((double) value) <= ((double) needle)) {
                    return true;
                } else if (clazz == Long.class && ((long) value) <= ((long) needle)) {
                    return true;
                } else if (clazz == Float.class && ((float) value) <= ((float) needle)) {
                    return true;
                } else if (clazz == Short.class && ((short) value) <= ((short) needle)) {
                    return true;
                }
            } else if (condition == Compare.more) {

                if (clazz == Date.class && (((Date) value)).compareTo((Date) needle) > 0) {
                    return true;
                } else if (clazz == Byte.class && ((byte) value) > ((byte) needle)) {
                    return true;
                } else if (clazz == Integer.class && ((int) value) > ((int) needle)) {
                    return true;
                } else if (clazz == Double.class && ((double) value) > ((double) needle)) {
                    return true;
                } else if (clazz == Long.class && ((long) value) > ((long) needle)) {
                    return true;
                } else if (clazz == Float.class && ((float) value) > ((float) needle)) {
                    return true;
                } else if (clazz == Short.class && ((short) value) > ((short) needle)) {
                    return true;
                }
            } else if (condition == Compare.moreOrEqual) {

                if (clazz == Date.class && (((Date) value)).compareTo((Date) needle) >= 0) {
                    return true;
                } else if (clazz == Byte.class && ((byte) value) >= ((byte) needle)) {
                    return true;
                } else if (clazz == Integer.class && ((int) value) >= ((int) needle)) {
                    return true;
                } else if (clazz == Double.class && ((double) value) >= ((double) needle)) {
                    return true;
                } else if (clazz == Long.class && ((long) value) >= ((long) needle)) {
                    return true;
                } else if (clazz == Float.class && ((float) value) >= ((float) needle)) {
                    return true;
                } else if (clazz == Short.class && ((short) value) >= ((short) needle)) {
                    return true;
                }
            } else if (condition == Compare.between) {

                if (clazz == Date.class) {

                    if (((Date) value).getTime() >= ((Date) inLeft).getTime() && ((Date) value).getTime() <= ((Date) inRight).getTime())
                        return true;
                } else if (clazz == Integer.class && ((int) value) >= ((int) inLeft) && ((int) value) <= ((int) inRight)) {
                    return true;
                } else if (clazz == Double.class && ((double) value) >= ((double) inLeft) && ((double) value) <= ((double) inRight)) {
                    return true;
                } else if (clazz == Long.class && ((long) value) >= ((long) inLeft) && ((long) value) <= ((long) inRight)) {
                    return true;
                } else if (clazz == Float.class && ((float) value) >= ((float) inLeft) && ((float) value) <= ((float) inRight)) {
                    return true;
                } else if (clazz == Short.class && ((short) value) >= ((short) inLeft) && ((short) value) <= ((short) inRight)) {
                    return true;
                }
            } else if (condition == Compare.in) {

                if (clazz == Date[].class) {

                    for (Object needle : needles) {
                        if ((((Date) value)).compareTo((Date) needle) == 0)
                            return true;
                    }
                } else {
                    if (clazz == String[].class && casing == Case.INSENSITIVE) {
                        return needles.contains(((String) value).toLowerCase());
                    } else {
                        return needles.contains(value);
                    }
                }
            } else if (clazz == String.class && (condition == Compare.contains || condition == Compare.endsWith)) {

                if (condition == Compare.contains) {
                    if (casing == Case.SENSITIVE && ((String) value).contains((String) needle)) {
                        return true;
                    } else if (casing == Case.INSENSITIVE && (((String) value).toLowerCase()).contains(((String) needle))) {
                        return true;
                    }
                } else if (condition == Compare.endsWith) {
                    if (casing == Case.SENSITIVE && ((String) value).endsWith((String) needle)) {
                        return true;
                    } else if (casing == Case.INSENSITIVE && (((String) value).toLowerCase()).endsWith(((String) needle))) {
                        return true;
                    }
                }
            }else if( condition == Compare.isEmpty ){

                if( value instanceof String || value instanceof AbstractList ){
                    if( value instanceof String && ((String)value).isEmpty() )
                        return true;
                    else
                    if( value instanceof AbstractList && ((AbstractList)value).isEmpty() )
                        return true;
                }else{
                    throw new RealmException( "Field '" + types.get(level) + "': type mismatch. Was OBJECT, expected [STRING, BINARY, LIST]" );
                }
            }
        }else if( condition == Compare.isNull){
            return true;
        }

        return false;
    }
}
