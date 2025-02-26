/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.pojos;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class CeRecommendations implements Serializable {
  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String namespace;
  private Double monthlycost;
  private Double monthlysaving;
  private String clustername;
  private String resourcetype;
  private String accountid;
  private Boolean isvalid;
  private OffsetDateTime lastprocessedat;
  private OffsetDateTime updatedat;

  public CeRecommendations() {}

  public CeRecommendations(CeRecommendations value) {
    this.id = value.id;
    this.name = value.name;
    this.namespace = value.namespace;
    this.monthlycost = value.monthlycost;
    this.monthlysaving = value.monthlysaving;
    this.clustername = value.clustername;
    this.resourcetype = value.resourcetype;
    this.accountid = value.accountid;
    this.isvalid = value.isvalid;
    this.lastprocessedat = value.lastprocessedat;
    this.updatedat = value.updatedat;
  }

  public CeRecommendations(String id, String name, String namespace, Double monthlycost, Double monthlysaving,
      String clustername, String resourcetype, String accountid, Boolean isvalid, OffsetDateTime lastprocessedat,
      OffsetDateTime updatedat) {
    this.id = id;
    this.name = name;
    this.namespace = namespace;
    this.monthlycost = monthlycost;
    this.monthlysaving = monthlysaving;
    this.clustername = clustername;
    this.resourcetype = resourcetype;
    this.accountid = accountid;
    this.isvalid = isvalid;
    this.lastprocessedat = lastprocessedat;
    this.updatedat = updatedat;
  }

  /**
   * Getter for <code>public.ce_recommendations.id</code>.
   */
  public String getId() {
    return this.id;
  }

  /**
   * Setter for <code>public.ce_recommendations.id</code>.
   */
  public CeRecommendations setId(String id) {
    this.id = id;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.name</code>.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Setter for <code>public.ce_recommendations.name</code>.
   */
  public CeRecommendations setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.namespace</code>.
   */
  public String getNamespace() {
    return this.namespace;
  }

  /**
   * Setter for <code>public.ce_recommendations.namespace</code>.
   */
  public CeRecommendations setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.monthlycost</code>.
   */
  public Double getMonthlycost() {
    return this.monthlycost;
  }

  /**
   * Setter for <code>public.ce_recommendations.monthlycost</code>.
   */
  public CeRecommendations setMonthlycost(Double monthlycost) {
    this.monthlycost = monthlycost;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.monthlysaving</code>.
   */
  public Double getMonthlysaving() {
    return this.monthlysaving;
  }

  /**
   * Setter for <code>public.ce_recommendations.monthlysaving</code>.
   */
  public CeRecommendations setMonthlysaving(Double monthlysaving) {
    this.monthlysaving = monthlysaving;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.clustername</code>.
   */
  public String getClustername() {
    return this.clustername;
  }

  /**
   * Setter for <code>public.ce_recommendations.clustername</code>.
   */
  public CeRecommendations setClustername(String clustername) {
    this.clustername = clustername;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.resourcetype</code>.
   */
  public String getResourcetype() {
    return this.resourcetype;
  }

  /**
   * Setter for <code>public.ce_recommendations.resourcetype</code>.
   */
  public CeRecommendations setResourcetype(String resourcetype) {
    this.resourcetype = resourcetype;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.accountid</code>.
   */
  public String getAccountid() {
    return this.accountid;
  }

  /**
   * Setter for <code>public.ce_recommendations.accountid</code>.
   */
  public CeRecommendations setAccountid(String accountid) {
    this.accountid = accountid;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.isvalid</code>.
   */
  public Boolean getIsvalid() {
    return this.isvalid;
  }

  /**
   * Setter for <code>public.ce_recommendations.isvalid</code>.
   */
  public CeRecommendations setIsvalid(Boolean isvalid) {
    this.isvalid = isvalid;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.lastprocessedat</code>.
   */
  public OffsetDateTime getLastprocessedat() {
    return this.lastprocessedat;
  }

  /**
   * Setter for <code>public.ce_recommendations.lastprocessedat</code>.
   */
  public CeRecommendations setLastprocessedat(OffsetDateTime lastprocessedat) {
    this.lastprocessedat = lastprocessedat;
    return this;
  }

  /**
   * Getter for <code>public.ce_recommendations.updatedat</code>.
   */
  public OffsetDateTime getUpdatedat() {
    return this.updatedat;
  }

  /**
   * Setter for <code>public.ce_recommendations.updatedat</code>.
   */
  public CeRecommendations setUpdatedat(OffsetDateTime updatedat) {
    this.updatedat = updatedat;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final CeRecommendations other = (CeRecommendations) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (namespace == null) {
      if (other.namespace != null)
        return false;
    } else if (!namespace.equals(other.namespace))
      return false;
    if (monthlycost == null) {
      if (other.monthlycost != null)
        return false;
    } else if (!monthlycost.equals(other.monthlycost))
      return false;
    if (monthlysaving == null) {
      if (other.monthlysaving != null)
        return false;
    } else if (!monthlysaving.equals(other.monthlysaving))
      return false;
    if (clustername == null) {
      if (other.clustername != null)
        return false;
    } else if (!clustername.equals(other.clustername))
      return false;
    if (resourcetype == null) {
      if (other.resourcetype != null)
        return false;
    } else if (!resourcetype.equals(other.resourcetype))
      return false;
    if (accountid == null) {
      if (other.accountid != null)
        return false;
    } else if (!accountid.equals(other.accountid))
      return false;
    if (isvalid == null) {
      if (other.isvalid != null)
        return false;
    } else if (!isvalid.equals(other.isvalid))
      return false;
    if (lastprocessedat == null) {
      if (other.lastprocessedat != null)
        return false;
    } else if (!lastprocessedat.equals(other.lastprocessedat))
      return false;
    if (updatedat == null) {
      if (other.updatedat != null)
        return false;
    } else if (!updatedat.equals(other.updatedat))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
    result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
    result = prime * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
    result = prime * result + ((this.monthlycost == null) ? 0 : this.monthlycost.hashCode());
    result = prime * result + ((this.monthlysaving == null) ? 0 : this.monthlysaving.hashCode());
    result = prime * result + ((this.clustername == null) ? 0 : this.clustername.hashCode());
    result = prime * result + ((this.resourcetype == null) ? 0 : this.resourcetype.hashCode());
    result = prime * result + ((this.accountid == null) ? 0 : this.accountid.hashCode());
    result = prime * result + ((this.isvalid == null) ? 0 : this.isvalid.hashCode());
    result = prime * result + ((this.lastprocessedat == null) ? 0 : this.lastprocessedat.hashCode());
    result = prime * result + ((this.updatedat == null) ? 0 : this.updatedat.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("CeRecommendations (");

    sb.append(id);
    sb.append(", ").append(name);
    sb.append(", ").append(namespace);
    sb.append(", ").append(monthlycost);
    sb.append(", ").append(monthlysaving);
    sb.append(", ").append(clustername);
    sb.append(", ").append(resourcetype);
    sb.append(", ").append(accountid);
    sb.append(", ").append(isvalid);
    sb.append(", ").append(lastprocessedat);
    sb.append(", ").append(updatedat);

    sb.append(")");
    return sb.toString();
  }
}
