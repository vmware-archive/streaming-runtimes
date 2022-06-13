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
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusConditions;
import com.vmware.tanzu.streaming.models.V1alpha1ClusterStreamStatusStorageAddress;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * V1alpha1ClusterStreamStatus
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-07-09T10:43:22.614Z[Etc/UTC]")
public class V1alpha1ClusterStreamStatus {
  public static final String SERIALIZED_NAME_BINDING = "binding";
  @SerializedName(SERIALIZED_NAME_BINDING)
  private V1alpha1ClusterStreamStatusBinding binding;

  public static final String SERIALIZED_NAME_CONDITIONS = "conditions";
  @SerializedName(SERIALIZED_NAME_CONDITIONS)
  private List<V1alpha1ClusterStreamStatusConditions> conditions = null;

  public static final String SERIALIZED_NAME_STORAGE_ADDRESS = "storageAddress";
  @SerializedName(SERIALIZED_NAME_STORAGE_ADDRESS)
  private V1alpha1ClusterStreamStatusStorageAddress storageAddress;


  public V1alpha1ClusterStreamStatus binding(V1alpha1ClusterStreamStatusBinding binding) {
    
    this.binding = binding;
    return this;
  }

   /**
   * Get binding
   * @return binding
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1alpha1ClusterStreamStatusBinding getBinding() {
    return binding;
  }


  public void setBinding(V1alpha1ClusterStreamStatusBinding binding) {
    this.binding = binding;
  }


  public V1alpha1ClusterStreamStatus conditions(List<V1alpha1ClusterStreamStatusConditions> conditions) {
    
    this.conditions = conditions;
    return this;
  }

  public V1alpha1ClusterStreamStatus addConditionsItem(V1alpha1ClusterStreamStatusConditions conditionsItem) {
    if (this.conditions == null) {
      this.conditions = new ArrayList<>();
    }
    this.conditions.add(conditionsItem);
    return this;
  }

   /**
   * List of status conditions.
   * @return conditions
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "List of status conditions.")

  public List<V1alpha1ClusterStreamStatusConditions> getConditions() {
    return conditions;
  }


  public void setConditions(List<V1alpha1ClusterStreamStatusConditions> conditions) {
    this.conditions = conditions;
  }


  public V1alpha1ClusterStreamStatus storageAddress(V1alpha1ClusterStreamStatusStorageAddress storageAddress) {
    
    this.storageAddress = storageAddress;
    return this;
  }

   /**
   * Get storageAddress
   * @return storageAddress
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public V1alpha1ClusterStreamStatusStorageAddress getStorageAddress() {
    return storageAddress;
  }


  public void setStorageAddress(V1alpha1ClusterStreamStatusStorageAddress storageAddress) {
    this.storageAddress = storageAddress;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1alpha1ClusterStreamStatus v1alpha1ClusterStreamStatus = (V1alpha1ClusterStreamStatus) o;
    return Objects.equals(this.binding, v1alpha1ClusterStreamStatus.binding) &&
        Objects.equals(this.conditions, v1alpha1ClusterStreamStatus.conditions) &&
        Objects.equals(this.storageAddress, v1alpha1ClusterStreamStatus.storageAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(binding, conditions, storageAddress);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1alpha1ClusterStreamStatus {\n");
    sb.append("    binding: ").append(toIndentedString(binding)).append("\n");
    sb.append("    conditions: ").append(toIndentedString(conditions)).append("\n");
    sb.append("    storageAddress: ").append(toIndentedString(storageAddress)).append("\n");
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

