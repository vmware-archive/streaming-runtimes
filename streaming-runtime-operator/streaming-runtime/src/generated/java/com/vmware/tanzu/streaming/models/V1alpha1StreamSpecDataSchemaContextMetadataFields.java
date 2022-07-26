/*
 * Kubernetes
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1.21.1
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
import com.vmware.tanzu.streaming.models.V1alpha1StreamSpecDataSchemaContextMetadata;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * V1alpha1StreamSpecDataSchemaContextMetadataFields
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-07-26T19:21:46.226Z[Etc/UTC]")
public class V1alpha1StreamSpecDataSchemaContextMetadataFields {
  public static final String SERIALIZED_NAME_LOGICAL_TYPE = "logicalType";
  @SerializedName(SERIALIZED_NAME_LOGICAL_TYPE)
  private String logicalType;

  public static final String SERIALIZED_NAME_METADATA = "metadata";
  @SerializedName(SERIALIZED_NAME_METADATA)
  private V1alpha1StreamSpecDataSchemaContextMetadata metadata;

  public static final String SERIALIZED_NAME_NAME = "name";
  @SerializedName(SERIALIZED_NAME_NAME)
  private String name;

  public static final String SERIALIZED_NAME_OPTIONAL = "optional";
  @SerializedName(SERIALIZED_NAME_OPTIONAL)
  private Boolean optional;

  public static final String SERIALIZED_NAME_TYPE = "type";
  @SerializedName(SERIALIZED_NAME_TYPE)
  private String type;

  public static final String SERIALIZED_NAME_WATERMARK = "watermark";
  @SerializedName(SERIALIZED_NAME_WATERMARK)
  private String watermark;


  public V1alpha1StreamSpecDataSchemaContextMetadataFields logicalType(String logicalType) {
    
    this.logicalType = logicalType;
    return this;
  }

   /**
   * Get logicalType
   * @return logicalType
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getLogicalType() {
    return logicalType;
  }


  public void setLogicalType(String logicalType) {
    this.logicalType = logicalType;
  }


  public V1alpha1StreamSpecDataSchemaContextMetadataFields metadata(V1alpha1StreamSpecDataSchemaContextMetadata metadata) {
    
    this.metadata = metadata;
    return this;
  }

   /**
   * Get metadata
   * @return metadata
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1alpha1StreamSpecDataSchemaContextMetadata getMetadata() {
    return metadata;
  }


  public void setMetadata(V1alpha1StreamSpecDataSchemaContextMetadata metadata) {
    this.metadata = metadata;
  }


  public V1alpha1StreamSpecDataSchemaContextMetadataFields name(String name) {
    
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


  public V1alpha1StreamSpecDataSchemaContextMetadataFields optional(Boolean optional) {
    
    this.optional = optional;
    return this;
  }

   /**
   * Get optional
   * @return optional
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Boolean getOptional() {
    return optional;
  }


  public void setOptional(Boolean optional) {
    this.optional = optional;
  }


  public V1alpha1StreamSpecDataSchemaContextMetadataFields type(String type) {
    
    this.type = type;
    return this;
  }

   /**
   * Get type
   * @return type
  **/
  @ApiModelProperty(required = true, value = "")

  public String getType() {
    return type;
  }


  public void setType(String type) {
    this.type = type;
  }


  public V1alpha1StreamSpecDataSchemaContextMetadataFields watermark(String watermark) {
    
    this.watermark = watermark;
    return this;
  }

   /**
   * Get watermark
   * @return watermark
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getWatermark() {
    return watermark;
  }


  public void setWatermark(String watermark) {
    this.watermark = watermark;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1StreamSpecDataSchemaContextMetadataFields v1alpha1StreamSpecDataSchemaContextMetadataFields = (V1alpha1StreamSpecDataSchemaContextMetadataFields) o;
    return Objects.equals(this.logicalType, v1alpha1StreamSpecDataSchemaContextMetadataFields.logicalType) &&
        Objects.equals(this.metadata, v1alpha1StreamSpecDataSchemaContextMetadataFields.metadata) &&
        Objects.equals(this.name, v1alpha1StreamSpecDataSchemaContextMetadataFields.name) &&
        Objects.equals(this.optional, v1alpha1StreamSpecDataSchemaContextMetadataFields.optional) &&
        Objects.equals(this.type, v1alpha1StreamSpecDataSchemaContextMetadataFields.type) &&
        Objects.equals(this.watermark, v1alpha1StreamSpecDataSchemaContextMetadataFields.watermark);
  }

  @Override
  public int hashCode() {
    return Objects.hash(logicalType, metadata, name, optional, type, watermark);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1StreamSpecDataSchemaContextMetadataFields {\n");
    sb.append("    logicalType: ").append(toIndentedString(logicalType)).append("\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    optional: ").append(toIndentedString(optional)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    watermark: ").append(toIndentedString(watermark)).append("\n");
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

