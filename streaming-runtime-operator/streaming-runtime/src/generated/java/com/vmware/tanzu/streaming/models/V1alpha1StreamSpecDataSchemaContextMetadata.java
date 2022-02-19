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
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * V1alpha1StreamSpecDataSchemaContextMetadata
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T17:19:50.021Z[Etc/UTC]")
public class V1alpha1StreamSpecDataSchemaContextMetadata {
  public static final String SERIALIZED_NAME_FROM = "from";
  @SerializedName(SERIALIZED_NAME_FROM)
  private String from;

  public static final String SERIALIZED_NAME_READONLY = "readonly";
  @SerializedName(SERIALIZED_NAME_READONLY)
  private Boolean readonly;


  public V1alpha1StreamSpecDataSchemaContextMetadata from(String from) {
    
    this.from = from;
    return this;
  }

   /**
   * Get from
   * @return from
  **/
  @ApiModelProperty(required = true, value = "")

  public String getFrom() {
    return from;
  }


  public void setFrom(String from) {
    this.from = from;
  }


  public V1alpha1StreamSpecDataSchemaContextMetadata readonly(Boolean readonly) {
    
    this.readonly = readonly;
    return this;
  }

   /**
   * Get readonly
   * @return readonly
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Boolean getReadonly() {
    return readonly;
  }


  public void setReadonly(Boolean readonly) {
    this.readonly = readonly;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1StreamSpecDataSchemaContextMetadata v1alpha1StreamSpecDataSchemaContextMetadata = (V1alpha1StreamSpecDataSchemaContextMetadata) o;
    return Objects.equals(this.from, v1alpha1StreamSpecDataSchemaContextMetadata.from) &&
        Objects.equals(this.readonly, v1alpha1StreamSpecDataSchemaContextMetadata.readonly);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, readonly);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1StreamSpecDataSchemaContextMetadata {\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    readonly: ").append(toIndentedString(readonly)).append("\n");
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

