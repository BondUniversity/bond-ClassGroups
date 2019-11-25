<!DOCTYPE html>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="/bbNG" prefix="bbNG"%>
<%@taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="bbng" uri="/bbNG" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<fmt:message var="groupSynchronisation" key="bond.classgroups.editgroup.groupsynchronisation" />
<fmt:message var="groupSynchronisationInstrutions" key="bond.classgroups.editgroup.groupsynchronisation.instructions" />
<fmt:message var="synchronised" key="bond.classgroups.editgroup.groupsynchronisation.synchronised" />
<fmt:message var="synchronisedDescription" key="bond.classgroups.editgroup.groupsynchronisation.synchroniseddescription" />
<fmt:message var="blocked" key="bond.classgroups.editgroup.groupsynchronisation.blocked" />
<fmt:message var="blockedDescription" key="bond.classgroups.editgroup.groupsynchronisation.blockeddescription" />

<fmt:message var="groupLeader" key="bond.classgroups.editgroup.groupleader" />
<fmt:message var="groupLeaderInstructions" key="bond.classgroups.editgroup.groupleader.instructions" />
<fmt:message var="feed" key="bond.classgroups.editgroup.groupleader.feed" />
<fmt:message var="feedDescription" key="bond.classgroups.editgroup.groupleader.feeddescription" />
<fmt:message var="noLeader" key="bond.classgroups.editgroup.groupleader.noleader" />

<fmt:message var="override" key="bond.classgroups.editgroup.groupleader.override" />
<fmt:message var="overrideDescription" key="bond.classgroups.editgroup.groupleader.overridedescription" />
<fmt:message var="selectUserButton" key="bond.classgroups.editgroup.groupleader.selectuserbutton" />

<fmt:message var="saveButton" key="bond.classgroups.editgroup.saveButton" />


<bbNG:learningSystemPage ctxId="ctx" navItem="bond-ClassGroupsCourseSettings-nav-courseSettings">

    <bbNG:cssFile href="css/default.css" />
    <bbNG:jsFile href="js/editgroup.js" />

    <bbNG:form method="post" name="configureForm" action="CourseSettings.action?saveGroup&course_id=${actionBean.courseId}&group=${actionBean.group}">
        <bbNG:dataCollection>
            <bbNG:step title="${groupSynchronisation}" instructions="${groupSynchronisationInstrutions}">
                <bbNG:dataElement isRequired="true">
                    <ul>
                        <li><label><input type="radio" name="groupExtension.synced" value="true" ${actionBean.groupExtension.synced?"checked=checked":""} /> <strong>${synchronised}</strong>: ${synchronisedDescription}</label></li>
                        <li><label><input type="radio" name="groupExtension.synced" value="false" ${!actionBean.groupExtension.synced?"checked=checked":""} /> <strong>${blocked}</strong>: ${blockedDescription}</label></li>
                    </ul>
                </bbNG:dataElement>
            </bbNG:step>

            <bbNG:step title="${groupLeader}" instructions="${groupLeaderInstructions}">
                <%--<bbNG:step title="${feedLeader}" subStep="true">--%>
                    <bbNG:dataElement>
                        <div><label><input type="radio" name="groupExtension.leaderOverridden" value="false" ${!actionBean.groupExtension.leaderOverridden?"checked=checked":""} /> <strong>${feed}</strong>: ${feedDescription}</label></div>
                        <div class="leader">
                            <div class="leaderContent">
                                <c:choose>
                                    <c:when test="${actionBean.feedLeader != null}">
                                        <fmt:message var="leaderName" key="bond.classgroups.editgroup.groupleader.leadername">
                                            <fmt:param value="${actionBean.feedLeader.givenName}" />
                                            <fmt:param value="${actionBean.feedLeader.familyName}" />
                                        </fmt:message>
                                        ${leaderName}
                                    </c:when>
                                    <c:otherwise>${noLeader}</c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </bbNG:dataElement>
                <%--</bbNG:step>--%>
                <%--<bbNG:step title="${overrideLeader}" subStep="true">--%>
                    <bbNG:dataElement>
                        <div><label><input type="radio" id="leaderOverriddenTrue" name="groupExtension.leaderOverridden" value="true" ${actionBean.groupExtension.leaderOverridden?"checked=checked":""} /> <strong>${override}</strong>: ${overrideDescription}</label></div>
                        <div class="leader">
                            <div id="overriddenLeaderDisplay" class="leaderContent">
                                <c:choose>
                                    <c:when test="${actionBean.overriddenLeader != null}">
                                        <fmt:message var="leaderName" key="bond.classgroups.editgroup.groupleader.leadername">
                                            <fmt:param value="${actionBean.overriddenLeader.givenName}" />
                                            <fmt:param value="${actionBean.overriddenLeader.familyName}" />
                                        </fmt:message>
                                        ${leaderName}
                                    </c:when>
                                    <c:otherwise>${noLeader}</c:otherwise>
                                </c:choose>
                            </div>
                            <bbNG:multiUserPicker
                                    displayType="BUTTON"
                                    selectMultiple="false"
                                    javascriptName="handleOverriddenUserSelect"
                                    label="${selectUserButton}"
                                    hideGuest="true"
                                    />
                        </div>
                        <input type="hidden" name="newOverriddenLeader" id="newOverriddenLeader" value="" />

                    </bbNG:dataElement>
                <%--</bbNG:step>--%>
            </bbNG:step>

            <bbNG:stepSubmit showCancelButton="true">
                <bbNG:stepSubmitButton label="${saveButton}" id="submitSave"/>
            </bbNG:stepSubmit>
        </bbNG:dataCollection>
    </bbNG:form>

</bbNG:learningSystemPage>