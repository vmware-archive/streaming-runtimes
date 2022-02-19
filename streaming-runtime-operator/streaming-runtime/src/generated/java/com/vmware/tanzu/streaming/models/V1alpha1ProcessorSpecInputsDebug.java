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
import java.util.ArrayList;
import java.util.List;

/**
 * V1alpha1ProcessorSpecInputsDebug
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-02-19T17:19:50.021Z[Etc/UTC]")
public class V1alpha1ProcessorSpecInputsDebug {
  public static final String SERIALIZED_NAME_EXPLAIN = "explain";
  @SerializedName(SERIALIZED_NAME_EXPLAIN)
  private List<Integer> explain = null;

  public static final String SERIALIZED_NAME_QUERY = "query";
  @SerializedName(SERIALIZED_NAME_QUERY)
  private String query;


  public V1alpha1ProcessorSpecInputsDebug explain(List<Integer> explain) {
    
    this.explain = explain;
    return this;
  }

  public V1alpha1ProcessorSpecInputsDebug addExplainItem(Integer explainItem) {
    if (this.explain == null) {
      this.explain = new ArrayList<>();
    }
    this.explain.add(explainItem);
    return this;
  }

   /**
   * Get explain
   * @return explain
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<Integer> getExplain() {
    return explain;
  }


  public void setExplain(List<Integer> explain) {
    this.explain = explain;
  }


  public V1alpha1ProcessorSpecInputsDebug query(String query) {
    
    this.query = query;
    return this;
  }

   /**
   * Get query
   * @return query
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getQuery() {
    return query;
  }


  public void setQuery(String query) {
    this.query = query;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1ProcessorSpecInputsDebug v1alpha1ProcessorSpecInputsDebug = (V1alpha1ProcessorSpecInputsDebug) o;
    return Objects.equals(this.explain, v1alpha1ProcessorSpecInputsDebug.explain) &&
        Objects.equals(this.query, v1alpha1ProcessorSpecInputsDebug.query);
  }

  @Override
  public int hashCode() {
    return Objects.hash(explain, query);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1ProcessorSpecInputsDebug {\n");
    sb.append("    explain: ").append(toIndentedString(explain)).append("\n");
    sb.append("    query: ").append(toIndentedString(query)).append("\n");
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

