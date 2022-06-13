/*
 * Kubernetes
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1.19.11
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.vmware.tanzu.streaming.models;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecDataSchemaContextMetadataFields;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V1alpha1StreamSpecDataSchemaContextSchema
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-07-09T10:43:22.614Z[Etc/UTC]")
public class V1alpha1StreamSpecDataSchemaContextSchema {
  public static final String SERIALIZED_NAME_FIELDS = "fields";
  @SerializedName(SERIALIZED_NAME_FIELDS)
  private List<V1alpha1StreamSpecDataSchemaContextMetadataFields> fields = new ArrayList<>();

  public static final String SERIALIZED_NAME_NAME = "name";
  @SerializedName(SERIALIZED_NAME_NAME)
  private String name;

  public static final String SERIALIZED_NAME_NAMESPACE = "namespace";
  @SerializedName(SERIALIZED_NAME_NAMESPACE)
  private String namespace;


  public V1alpha1StreamSpecDataSchemaContextSchema fields(List<V1alpha1StreamSpecDataSchemaContextMetadataFields> fields) {
    
    this.fields = fields;
    return this;
  }

  public V1alpha1StreamSpecDataSchemaContextSchema addFieldsItem(V1alpha1StreamSpecDataSchemaContextMetadataFields fieldsItem) {
    this.fields.add(fieldsItem);
    return this;
  }

   /**
   * Get fields
   * @return fields
  **/
  @ApiModelProperty(required = true, value = "")

  public List<V1alpha1StreamSpecDataSchemaContextMetadataFields> getFields() {
    return fields;
  }


  public void setFields(List<V1alpha1StreamSpecDataSchemaContextMetadataFields> fields) {
    this.fields = fields;
  }


  public V1alpha1StreamSpecDataSchemaContextSchema name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @ApiModelProperty(required = true, value = "")

  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public V1alpha1StreamSpecDataSchemaContextSchema namespace(String namespace) {
    
    this.namespace = namespace;
    return this;
  }

   /**
   * Get namespace
   * @return namespace
  **/
  @ApiModelProperty(required = true, value = "")

  public String getNamespace() {
    return namespace;
  }


  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1StreamSpecDataSchemaContextSchema v1alpha1StreamSpecDataSchemaContextSchema = (V1alpha1StreamSpecDataSchemaContextSchema) o;
    return Objects.equals(this.fields, v1alpha1StreamSpecDataSchemaContextSchema.fields) &&
        Objects.equals(this.name, v1alpha1StreamSpecDataSchemaContextSchema.name) &&
        Objects.equals(this.namespace, v1alpha1StreamSpecDataSchemaContextSchema.namespace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fields, name, namespace);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1StreamSpecDataSchemaContextSchema {\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    namespace: ").append(toIndentedString(namespace)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

