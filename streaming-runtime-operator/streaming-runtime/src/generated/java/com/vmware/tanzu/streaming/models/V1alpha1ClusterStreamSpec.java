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
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamSpecStorage;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V1alpha1ClusterStreamSpec
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T17:19:50.021Z[Etc/UTC]")
public class V1alpha1ClusterStreamSpec {
  public static final String SERIALIZED_NAME_KEYS = "keys";
  @SerializedName(SERIALIZED_NAME_KEYS)
  private List<String> keys = null;

  public static final String SERIALIZED_NAME_STORAGE = "storage";
  @SerializedName(SERIALIZED_NAME_STORAGE)
  private V1alpha1ClusterStreamSpecStorage storage;

  public static final String SERIALIZED_NAME_STREAM_MODES = "streamModes";
  @SerializedName(SERIALIZED_NAME_STREAM_MODES)
  private List<String> streamModes = null;


  public V1alpha1ClusterStreamSpec keys(List<String> keys) {
    
    this.keys = keys;
    return this;
  }

  public V1alpha1ClusterStreamSpec addKeysItem(String keysItem) {
    if (this.keys == null) {
      this.keys = new ArrayList<>();
    }
    this.keys.add(keysItem);
    return this;
  }

   /**
   * Get keys
   * @return keys
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<String> getKeys() {
    return keys;
  }


  public void setKeys(List<String> keys) {
    this.keys = keys;
  }


  public V1alpha1ClusterStreamSpec storage(V1alpha1ClusterStreamSpecStorage storage) {
    
    this.storage = storage;
    return this;
  }

   /**
   * Get storage
   * @return storage
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1alpha1ClusterStreamSpecStorage getStorage() {
    return storage;
  }


  public void setStorage(V1alpha1ClusterStreamSpecStorage storage) {
    this.storage = storage;
  }


  public V1alpha1ClusterStreamSpec streamModes(List<String> streamModes) {
    
    this.streamModes = streamModes;
    return this;
  }

  public V1alpha1ClusterStreamSpec addStreamModesItem(String streamModesItem) {
    if (this.streamModes == null) {
      this.streamModes = new ArrayList<>();
    }
    this.streamModes.add(streamModesItem);
    return this;
  }

   /**
   * Get streamModes
   * @return streamModes
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<String> getStreamModes() {
    return streamModes;
  }


  public void setStreamModes(List<String> streamModes) {
    this.streamModes = streamModes;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1ClusterStreamSpec v1alpha1ClusterStreamSpec = (V1alpha1ClusterStreamSpec) o;
    return Objects.equals(this.keys, v1alpha1ClusterStreamSpec.keys) &&
        Objects.equals(this.storage, v1alpha1ClusterStreamSpec.storage) &&
        Objects.equals(this.streamModes, v1alpha1ClusterStreamSpec.streamModes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(keys, storage, streamModes);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1ClusterStreamSpec {\n");
    sb.append("    keys: ").append(toIndentedString(keys)).append("\n");
    sb.append("    storage: ").append(toIndentedString(storage)).append("\n");
    sb.append("    streamModes: ").append(toIndentedString(streamModes)).append("\n");
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

