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
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusBinding;
import com.vmware.tanzu.streaming.models.V1alpha1ProcessorSpecInputsDebug;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V1alpha1ProcessorSpecInputs
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-05-27T12:06:58.846Z[Etc/UTC]")
public class V1alpha1ProcessorSpecInputs {
  public static final String SERIALIZED_NAME_DEBUG = "debug";
  @SerializedName(SERIALIZED_NAME_DEBUG)
  private V1alpha1ProcessorSpecInputsDebug debug;

  public static final String SERIALIZED_NAME_QUERY = "query";
  @SerializedName(SERIALIZED_NAME_QUERY)
  private List<String> query = null;

  public static final String SERIALIZED_NAME_SOURCES = "sources";
  @SerializedName(SERIALIZED_NAME_SOURCES)
  private List<V1alpha1ClusterStreamStatusBinding> sources = null;


  public V1alpha1ProcessorSpecInputs debug(V1alpha1ProcessorSpecInputsDebug debug) {
    
    this.debug = debug;
    return this;
  }

   /**
   * Get debug
   * @return debug
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1alpha1ProcessorSpecInputsDebug getDebug() {
    return debug;
  }


  public void setDebug(V1alpha1ProcessorSpecInputsDebug debug) {
    this.debug = debug;
  }


  public V1alpha1ProcessorSpecInputs query(List<String> query) {
    
    this.query = query;
    return this;
  }

  public V1alpha1ProcessorSpecInputs addQueryItem(String queryItem) {
    if (this.query == null) {
      this.query = new ArrayList<>();
    }
    this.query.add(queryItem);
    return this;
  }

   /**
   * Get query
   * @return query
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<String> getQuery() {
    return query;
  }


  public void setQuery(List<String> query) {
    this.query = query;
  }


  public V1alpha1ProcessorSpecInputs sources(List<V1alpha1ClusterStreamStatusBinding> sources) {
    
    this.sources = sources;
    return this;
  }

  public V1alpha1ProcessorSpecInputs addSourcesItem(V1alpha1ClusterStreamStatusBinding sourcesItem) {
    if (this.sources == null) {
      this.sources = new ArrayList<>();
    }
    this.sources.add(sourcesItem);
    return this;
  }

   /**
   * Get sources
   * @return sources
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<V1alpha1ClusterStreamStatusBinding> getSources() {
    return sources;
  }


  public void setSources(List<V1alpha1ClusterStreamStatusBinding> sources) {
    this.sources = sources;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1ProcessorSpecInputs v1alpha1ProcessorSpecInputs = (V1alpha1ProcessorSpecInputs) o;
    return Objects.equals(this.debug, v1alpha1ProcessorSpecInputs.debug) &&
        Objects.equals(this.query, v1alpha1ProcessorSpecInputs.query) &&
        Objects.equals(this.sources, v1alpha1ProcessorSpecInputs.sources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(debug, query, sources);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1ProcessorSpecInputs {\n");
    sb.append("    debug: ").append(toIndentedString(debug)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
    sb.append("    sources: ").append(toIndentedString(sources)).append("\n");
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

