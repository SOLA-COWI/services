/**
 * ******************************************************************************************
 * Copyright (C) 2012 - Food and Agriculture Organization of the United Nations (FAO).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,this list
 *       of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice,this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sola.services.ejb.search.repository.entities;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import org.sola.services.common.repository.ChildEntityList;
import org.sola.services.common.repository.entities.AbstractReadOnlyEntity;

/**
 *
 * @author soladev
 */
@Table(name = "query", schema = "system")
public class DynamicQuery extends AbstractReadOnlyEntity {

    @Id
    @Column
    private String name;
    @Column
    private String sql;
    @ChildEntityList(parentIdField = "queryName")
    List<DynamicQueryField> fieldList;

    public DynamicQuery() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<DynamicQueryField> getFieldList() {
        return fieldList;
    }

    public void setFieldList(List<DynamicQueryField> fieldList) {
        this.fieldList = fieldList;
    }

    public String[] getQueryFieldNames() {
        String[] fieldNames = null;
        if (fieldList != null && !fieldList.isEmpty()) {
            fieldNames = new String[fieldList.size()];
            for (DynamicQueryField field : fieldList) {
                // Assumes the index is 0 based and sequential with no gaps!
                fieldNames[field.getIndexInQuery()] = field.getName();
            }
        }
        return fieldNames;
    }

    public String[] getFieldDisplayNames() {
        String[] displayNames = null;
        if (fieldList != null && !fieldList.isEmpty()) {
            displayNames = new String[fieldList.size()];
            for (DynamicQueryField field : fieldList) {
                // Assumes the index is 0 based and sequential with no gaps!
                displayNames[field.getIndexInQuery()] = 
                        field.getDisplayValue()!= null?field.getDisplayValue():field.getName();
            }
        }
        return displayNames;
    }
}
