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
import com.vmware.tanzu.streaming.models.V1alpha1ProcessorSpecTemplateSpecContainers;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V1alpha1ProcessorSpecTemplateSpec
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-05-27T12:06:58.846Z[Etc/UTC]")
public class V1alpha1ProcessorSpecTemplateSpec {
  public static final String SERIALIZED_NAME_CONTAINERS = "containers";
  @SerializedName(SERIALIZED_NAME_CONTAINERS)
  private List<V1alpha1ProcessorSpecTemplateSpecContainers> containers = null;


  public V1alpha1ProcessorSpecTemplateSpec containers(List<V1alpha1ProcessorSpecTemplateSpecContainers> containers) {
    
    this.containers = containers;
    return this;
  }

  public V1alpha1ProcessorSpecTemplateSpec addContainersItem(V1alpha1ProcessorSpecTemplateSpecContainers containersItem) {
    if (this.containers == null) {
      this.containers = new ArrayList<>();
    }
    this.containers.add(containersItem);
    return this;
  }

   /**
   * Get containers
   * @return containers
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<V1alpha1ProcessorSpecTemplateSpecContainers> getContainers() {
    return containers;
  }


  public void setContainers(List<V1alpha1ProcessorSpecTemplateSpecContainers> containers) {
    this.containers = containers;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1ProcessorSpecTemplateSpec v1alpha1ProcessorSpecTemplateSpec = (V1alpha1ProcessorSpecTemplateSpec) o;
    return Objects.equals(this.containers, v1alpha1ProcessorSpecTemplateSpec.containers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(containers);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1ProcessorSpecTemplateSpec {\n");
    sb.append("    containers: ").append(toIndentedString(containers)).append("\n");
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

