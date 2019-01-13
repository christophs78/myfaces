/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.el;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.el.ELResolver;
import javax.enterprise.inject.spi.BeanManager;
import javax.faces.annotation.FacesConfig;
import javax.faces.context.FacesContext;

import org.apache.myfaces.cdi.config.FacesConfigBeanHolder;
import org.apache.myfaces.cdi.util.CDIUtils;
import org.apache.myfaces.config.RuntimeConfig;
import org.apache.myfaces.el.resolver.FacesCompositeELResolver.Scope;
import org.apache.myfaces.util.ExternalSpecifications;

/**
 * @author Mathias Broekelmann (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
public class ResolverBuilderBase
{
    private static final Logger log = Logger.getLogger(ResolverBuilderBase.class.getName());

    private final RuntimeConfig _config;

    public ResolverBuilderBase(RuntimeConfig config)
    {
        _config = config;
    }

    /**
     * add the el resolvers from the faces config, the el resolver wrapper for variable resolver, the el resolver
     * wrapper for the property resolver and the el resolvers added by
     * {@link javax.faces.application.Application#addELResolver(ELResolver)}.
     * The resolvers where only added if they are not null
     * 
     * @param resolvers
     */
    protected void addFromRuntimeConfig(List<ELResolver> resolvers)
    {
        if (_config.getFacesConfigElResolvers() != null)
        {
            resolvers.addAll(_config.getFacesConfigElResolvers());
        }

        if (_config.getApplicationElResolvers() != null)
        {
            resolvers.addAll(_config.getApplicationElResolvers());
        }
    }
    
    /**
     * Sort the ELResolvers with a custom Comparator provided by the user.
     * @param resolvers
     * @param scope scope of ELResolvers (Faces,JSP)  
     * @since 1.2.10, 2.0.2
     */
    @SuppressWarnings("unchecked")
    protected void sortELResolvers(List<ELResolver> resolvers, Scope scope)
    {
        if (_config.getELResolverComparator() != null)
        {
            try
            {
                // sort the resolvers
                Collections.sort(resolvers, _config.getELResolverComparator());
                
                if (log.isLoggable(Level.INFO))
                {
                    log.log(Level.INFO, "Chain of EL resolvers for {0} sorted with: {1} and the result order is {2}", 
                            new Object [] {scope, _config.getELResolverComparator(), resolvers});
                }
            }
            catch (Exception e)
            {
                log.log(Level.WARNING, 
                        "Could not sort ELResolvers with custom Comparator", e);
            }
        }
    }
    
    /**
     * Filters the ELResolvers  with a custom Predicate provided by the user.
     * @param resolvers list of ELResolvers
     * @param scope scope of ELResolvers (Faces,JSP)
     * @return Iterable instance of Iterable containing filtered ELResolvers 
     */
    protected Iterable<ELResolver> filterELResolvers(List<ELResolver> resolvers, Scope scope)
    {
        
        Predicate<ELResolver> predicate = _config.getELResolverPredicate();
        if (predicate != null)
        {
            try
            {
                // filter the resolvers
                resolvers.removeIf(elResolver -> !predicate.test(elResolver));

                if (log.isLoggable(Level.INFO))
                {
                    log.log(Level.INFO, "Chain of EL resolvers for {0} filtered with: {1} and the result is {2}", 
                            new Object [] {scope, predicate, resolvers});
                }
            }
            catch (Exception e)
            {
                log.log(Level.WARNING, 
                        "Could not filter ELResolvers with custom Predicate", e);
            }
        }
        return resolvers;
    }

    protected RuntimeConfig getRuntimeConfig()
    {
        return _config;
    }
    
    protected boolean isReplaceImplicitObjectResolverWithCDIResolver(FacesContext facesContext)
    {
        if (!ExternalSpecifications.isCDIAvailable(facesContext.getExternalContext()))
        {
            return false;
        }

        BeanManager beanManager = CDIUtils.getBeanManager(facesContext.getExternalContext());
        if (beanManager != null)
        {
            FacesConfigBeanHolder holder = CDIUtils.lookup(beanManager, FacesConfigBeanHolder.class);
            if (holder != null)
            {
                FacesConfig.Version version = holder.getFacesConfigVersion();
                if (version == null)
                {
                    return false;
                }
                else if (version.ordinal() >= FacesConfig.Version.JSF_2_3.ordinal())
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }
        return false;
    }
}