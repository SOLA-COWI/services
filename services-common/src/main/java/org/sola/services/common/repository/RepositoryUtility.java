/**
 * ******************************************************************************************
 * Copyright (C) 2012 - Food and Agriculture Organization of the United Nations
 * (FAO). All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,this
 * list of conditions and the following disclaimer. 2. Redistributions in binary
 * form must reproduce the above copyright notice,this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. 3. Neither the name of FAO nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT,STRICT LIABILITY,OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sola.services.common.repository;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import org.sola.common.SOLAException;
import org.sola.common.logging.LogUtility;
import org.sola.common.messaging.ServiceMessage;
import org.sola.services.common.ejbs.AbstractEJBLocal;
import org.sola.services.common.repository.entities.AbstractReadOnlyEntity;
import org.sola.services.common.repository.entities.ChildEntityInfo;
import org.sola.services.common.repository.entities.ColumnInfo;

/**
 *
 * @author soladev
 */
public class RepositoryUtility {

    private static Map<String, List<ColumnInfo>> entityColumns = new HashMap<String, List<ColumnInfo>>();
    private static Map<String, List<ColumnInfo>> entityIdColumns = new HashMap<String, List<ColumnInfo>>();
    private static Map<String, String> entityTableNames = new HashMap<String, String>();
    private static Map<String, String> sorterExpressions = new HashMap<String, String>();
    private static Map<String, List<ChildEntityInfo>> childEntities = new HashMap<String, List<ChildEntityInfo>>();

    public static void getAllFields(Class<?> c, List<Field> fields) {
        fields.addAll(Arrays.asList(c.getDeclaredFields()));
        Class<?> superClass = c.getSuperclass();
        if (superClass != null) {
            getAllFields(superClass, fields);
        }
    }

    public static <T extends AbstractReadOnlyEntity> String getTableName(Class<T> entityClass) {
        String tableName = null;
        if (entityTableNames.containsKey(entityClass.getName())) {
            tableName = entityTableNames.get(entityClass.getName());
        } else {
            Table tableAnnotation = entityClass.getAnnotation(Table.class);
            if (tableAnnotation != null) {
                tableName = tableAnnotation.schema() + "." + tableAnnotation.name();
                entityTableNames.put(entityClass.getName(), tableName);
            }
        }
        return tableName;
    }

    public static <T extends AbstractReadOnlyEntity> String getSorterExpression(Class<T> entityClass) {
        String sorterExpression = null;
        if (sorterExpressions.containsKey(entityClass.getName())) {
            sorterExpression = sorterExpressions.get(entityClass.getName());
        } else {
            DefaultSorter sorterAnnotation = entityClass.getAnnotation(DefaultSorter.class);
            if (sorterAnnotation != null) {
                sorterExpression = sorterAnnotation.sortString();
                sorterExpressions.put(entityClass.getName(), sorterExpression);
            }
        }
        return sorterExpression;
    }

    public static <T extends AbstractReadOnlyEntity> List<ColumnInfo> getColumns(Class<T> entityClass) {

        List<ColumnInfo> columns = new ArrayList<ColumnInfo>();
        if (entityColumns.containsKey(entityClass.getName())) {
            columns = entityColumns.get(entityClass.getName());
        } else {
            List<Field> allFields = new ArrayList<Field>();
            getAllFields(entityClass, allFields);

            for (Field field : allFields) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null) {
                    Boolean isId = (field.getAnnotation(Id.class) != null);
                    Boolean isLocalized = (field.getAnnotation(Localized.class) != null);
                    Class<?> fieldType = field.getType();
                    String columnName = columnAnnotation.name();
                    if (columnName == null || columnName.length() < 1) {
                        columnName = field.getName();
                    }
                    ColumnInfo columnInfo = new ColumnInfo(columnName,
                            field.getName(), fieldType, isId, isLocalized,
                            columnAnnotation.insertable(), columnAnnotation.updatable());
                    AccessFunctions accessFunctions = field.getAnnotation(AccessFunctions.class);
                    if (accessFunctions != null) {
                        columnInfo.setOnSelectFunction(accessFunctions.onSelect());
                        columnInfo.setOnChangeFunction(accessFunctions.onChange());
                    }
                    columns.add(columnInfo);
                }
            }
            entityColumns.put(entityClass.getName(), columns);
        }
        return columns;
    }

    public static <T extends AbstractReadOnlyEntity> ColumnInfo getColumnInfo(Class<T> entityClass,
            String fieldName) {
        ColumnInfo result = null;
        if (fieldName != null) {
            for (ColumnInfo columnInfo : getColumns(entityClass)) {
                if (columnInfo.getFieldName().equalsIgnoreCase(fieldName)) {
                    result = columnInfo;
                    break;
                }
            }
        }
        return result;
    }

    public static <T extends AbstractReadOnlyEntity> ChildEntityInfo getChildEntityInfo(Class<T> entityClass,
            String fieldName) {
        ChildEntityInfo result = null;
        if (fieldName != null) {
            for (ChildEntityInfo childInfo : getChildEntityInfo(entityClass)) {
                if (childInfo.getFieldName().equalsIgnoreCase(fieldName)) {
                    result = childInfo;
                    break;
                }
            }
        }
        return result;
    }

    public static <T extends AbstractReadOnlyEntity> Boolean isIdColumn(Class<T> entityClass,
            String fieldName) {
        Boolean result = false;
        if (fieldName != null) {
            for (ColumnInfo columnInfo : getIdColumns(entityClass)) {
                if (columnInfo.getFieldName().equalsIgnoreCase(fieldName)) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    public static <T extends AbstractReadOnlyEntity> List<ColumnInfo> getIdColumns(Class<T> entityClass) {
        return getIdColumns(entityClass, getColumns(entityClass));
    }

    public static <T extends AbstractReadOnlyEntity> List<ColumnInfo> getIdColumns(Class<T> entityClass,
            List<ColumnInfo> columns) {
        List<ColumnInfo> idColumns = new ArrayList<ColumnInfo>();
        if (entityIdColumns.containsKey(entityClass.getName())) {
            idColumns = entityIdColumns.get(entityClass.getName());
        } else {
            for (ColumnInfo columnInfo : columns) {
                if (columnInfo.isIdColumn()) {
                    idColumns.add(columnInfo);
                }
            }
            entityIdColumns.put(entityClass.getName(), idColumns);
        }
        return idColumns;
    }

    public static <T extends AbstractReadOnlyEntity> List<ChildEntityInfo> getChildEntityInfo(Class<T> entityClass) {

        List<ChildEntityInfo> children = new ArrayList<ChildEntityInfo>();
        if (childEntities.containsKey(entityClass.getName())) {
            children = childEntities.get(entityClass.getName());
        } else {
            List<Field> allFields = new ArrayList<Field>();
            getAllFields(entityClass, allFields);

            for (Field field : allFields) {
                ChildEntity childAnnotation = field.getAnnotation(ChildEntity.class);
                ChildEntityList childListAnnotation = field.getAnnotation(ChildEntityList.class);
                ExternalEJB externalEJBAnnoation = field.getAnnotation(ExternalEJB.class);
                ParameterizedType paramType = null;
                if (Iterable.class.isAssignableFrom(field.getType())) {
                    paramType = (ParameterizedType) field.getGenericType();
                }
                ChildEntityInfo childInfo = null;
                if (childAnnotation != null) {
                    boolean insert = childAnnotation.insertBeforeParent();

                    if ((insert && childAnnotation.childIdField().isEmpty())
                            || (!insert && childAnnotation.parentIdField().isEmpty())) {
                        throw new SOLAException(ServiceMessage.GENERAL_UNEXPECTED,
                                // The ChildEntity annoation is not configured correctly
                                new Object[]{"ChildEntity annotation is not configured correclty on "
                            + entityClass.getSimpleName() + "." + field.getName()});
                    }

                    childInfo = new ChildEntityInfo(field.getName(), field.getType(), insert,
                            childAnnotation.parentIdField(), childAnnotation.childIdField(),
                            childAnnotation.readOnly());
                }
                if (childListAnnotation != null) {
                    childInfo = new ChildEntityInfo(field.getName(), field.getType(), paramType,
                            childListAnnotation.parentIdField(), childListAnnotation.childIdField(),
                            childListAnnotation.manyToManyClass(),
                            childListAnnotation.cascadeDelete(),
                            childListAnnotation.readOnly());
                }
                if (externalEJBAnnoation != null && childInfo != null) {
                    childInfo.setEJBLocalClass(externalEJBAnnoation.ejbLocalClass());
                    childInfo.setLoadMethod(externalEJBAnnoation.loadMethod());
                    childInfo.setSaveMethod(externalEJBAnnoation.saveMethod());
                }
                if (childInfo != null) {
                    children.add(childInfo);
                }
            }
            childEntities.put(entityClass.getName(), children);
        }
        return children;
    }

    public static <T extends AbstractEJBLocal> T getEJB(Class<T> ejbLocalClass) {
        T ejb = null;

        String ejbLookupName = "java:global/SOLA/" + ejbLocalClass.getSimpleName();
        try {
            InitialContext ic = new InitialContext();
            ejb = (T) ic.lookup(ejbLookupName);
        } catch (NamingException ex) {
            throw new SOLAException(ServiceMessage.GENERAL_UNEXPECTED,
                    // Capture the specific details so they are added to the log
                    new Object[]{"Unable to locate EJB " + ejbLookupName, ex});
        }
        return ejb;
    }

    public static <T extends AbstractEJBLocal> T tryGetEJB(Class<T> ejbLocalClass) {
        T ejb = null;

        String ejbLookupName = "java:global/SOLA/" + ejbLocalClass.getSimpleName();
        try {
            InitialContext ic = new InitialContext();
            ejb = (T) ic.lookup(ejbLookupName);
        } catch (NamingException ex) {
            // Ignore the naming exception and return null; 
        }
        return ejb;
    }

    /**
     * Issue #192 Compare two arrays to determine if they are equal or not.
     * Performs a deep comparison of all array members using the Arrays.equal
     * method. Uses the array class to determine the correct cast to apply to
     * the object parameters.
     *
     * @param arrayClass The class for the array
     * @param array1 One of the arrays to compare
     * @param array2 The other array to compare
     * @return true if both arrays are equal
     */
    public static boolean arraysAreEqual(Class<?> arrayClass, Object array1, Object array2) {
        boolean result = false;
        if (byte[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((byte[]) array1, (byte[]) array2);
        } else if (Object[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((Object[]) array1, (Object[]) array2);
        } else if (int[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((int[]) array1, (int[]) array2);
        } else if (char[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((char[]) array1, (char[]) array2);
        } else if (long[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((long[]) array1, (long[]) array2);
        } else if (short[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((short[]) array1, (short[]) array2);
        } else if (boolean[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((boolean[]) array1, (boolean[]) array2);
        } else if (float[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((float[]) array1, (float[]) array2);
        } else if (double[].class.isAssignableFrom(arrayClass)) {
            result = Arrays.equals((double[]) array1, (double[]) array2);
        }
        return result;
    }

    /**
     * Issue #192 Compare two arrays to determine if they are equal or not.
     * Geometries are held as byte arrays, however a basic comparison of the
     * byte data does not give an accurate equals test due to Big Endian vs
     * Little Endian issues. Instead it is necessary to create the geometries
     * and use the explicit equals to their geometric equivalence.
     *
     * @param geom1 A geom to compare - can be NULL
     * @param geom2 The second geom to compare - can be NULL
     * @return TRUE if both geoms are NULL or geometrically equivalent. FALSE
     * otherwise.
     */
    public static boolean geometriesAreEqual(Object geom1, Object geom2) {
        boolean result = false;
        if (geom1 == null && geom2 == null) {
            result = true;
        } else if (geom1 != null && geom2 != null) {
            try {
                Geometry g1 = new WKBReader().read((byte[]) geom1);
                Geometry g2 = new WKBReader().read((byte[]) geom2);
                result = g1.equals(g2);
            } catch (ParseException ex) {
                LogUtility.log("Unable to compare geometries. Parse Error:" + ex.getMessage(), ex);
                result = false;
            }
        }
        return result;
    }
}
