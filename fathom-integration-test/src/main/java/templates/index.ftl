<#import "base.ftl" as base/>
<@base.page>
<div class="row">
    <div class="large-12 columns">
        <h1>${i18n('fathom.welcome')}</h1>
    </div>
</div>

<div class="row">
    <div class="large-6 columns">
        <h3>General API Routes</h3>

        <p>These routes are NOT GUARDED.</p>
        <#list items as item>
            <li><a href="${contextPath}/api/${item.id}">${item.name}</a></li>
        </#list>
    </div>
    <div class="large-6 columns">
        <h3>Secured API Routes</h3>

        <p>These routes are GUARDED by form authentication.</p>
        <ul>
            <#list items as item>
                <li><a href="${contextPath}/secure/${item.id}">${item.name}</a></li>
            </#list>
        </ul>
    </div>
</div>
<div class="row">
    <div class="large-6 columns">
        <h3>Other Sample Routes</h3>
        <ul>
            <li><a href="${contextPath}/notFound">An example 404</a></li>
            <li><a href="${contextPath}/internalError">An example 500</a></li>
            <li><a href="${contextPath}/secure/employees">${i18n('fathom.employees')}</a></li>
        </ul>
    </div>
    <div class="large-6 columns">
        <h3>Sample Controller Routes</h3>
        <ul>
            <li><a href="${contextPath}/instance">Singleton controller instance</a></li>
            <li><a href="${contextPath}/annotated">Annotated controller instance</a></li>
            <li><a href="${contextPath}/static">Static lambda controller method reference</a></li>
        </ul>
    </div>
</div>
<footer>
    <div class="row">
        <hr/>
        <b>${i18n('fathom.dispatchingSince')}</b> ${formatTime(bootDate, "full")}
        (<i>${prettyTime(bootDate)}</i>)
    </div>
</footer>
</@base.page>