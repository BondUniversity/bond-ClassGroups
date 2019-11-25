<!DOCTYPE html>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="/bbNG" prefix="bbNG"%>
<%@taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<fmt:message var="noTasks" key="bond.classgroups.tasklist.notasks" />
<fmt:message var="groupColumn" key="bond.classgroups.tasklist.groupcolumn" />
<fmt:message var="groupSetColumn" key="bond.classgroups.tasklist.groupsetcolumn" />
<fmt:message var="availableColumn" key="bond.classgroups.tasklist.availablecolumn" />
<fmt:message var="available" key="bond.classgroups.tasklist.available" />
<fmt:message var="unavailable" key="bond.classgroups.tasklist.unavailable" />
<fmt:message var="syncronisedColumn" key="bond.classgroups.tasklist.synchronisedcolumn" />
<fmt:message var="syncronised" key="bond.classgroups.tasklist.synchronised" />
<fmt:message var="blocked" key="bond.classgroups.tasklist.blocked" />
<fmt:message var="leaderOverriddenColumn" key="bond.classgroups.tasklist.leaderoverriddencolumn" />
<fmt:message var="leaderOverridden" key="bond.classgroups.tasklist.leaderoverridden" />
<fmt:message var="leaderNotOverridden" key="bond.classgroups.tasklist.leadernotoverridden" />
<fmt:message var="leaderColumn" key="bond.classgroups.tasklist.leadercolumn" />
<fmt:message var="noLeader" key="bond.classgroups.tasklist.noleader" />
<fmt:message var="nogroupset" key="bond.classgroups.tasklist.nogroupset" />

<bbNG:learningSystemPage ctxId="ctx" navItem="bond-ClassGroupsCourseSettings-nav-courseSettings">

    <bbNG:cssFile href="css/default.css" />

    <bbNG:pagedList collection="${actionBean.groupDataList}" className="au.edu.bond.classgroups.stripes.CourseSettingsAction.GroupData" objectVar="groupData"
                    recordCount="${actionBean.groupDataListCount}" displayPagingControls="true"
                    emptyMsg="${noTasks}" initialSortCol="group">
        <c:url var="editGroupLink" value="CourseSettings.action">
            <c:param name="editGroup" />
            <c:param name="course_id" value="${actionBean.courseId}" />
            <c:param name="group" value="${groupData.groupExtension.externalSystemId}" />
        </c:url>
        <bbNG:listElement name="group" isRowHeader="true" label="${groupColumn}" comparator="${actionBean.titleComparator}"><a href="${editGroupLink}">${groupData.bbGroup.title}</a></bbNG:listElement>
        <bbNG:listElement name="groupSet" label="${groupSetColumn}" comparator="${actionBean.setComparator}">${groupData.bbGroupSet != null ? groupData.bbGroupSet.title : nogroupset}</bbNG:listElement>
        <bbNG:listElement name="available" label="${availableColumn}" comparator="${actionBean.availableComparator}">${groupData.bbGroup.isAvailable ? available : unavailable}</bbNG:listElement>
        <bbNG:listElement name="syncronised" label="${syncronisedColumn}" comparator="${actionBean.syncComparator}">${groupData.groupExtension.synced ? syncronised : blocked}</bbNG:listElement>
        <bbNG:listElement name="leaderOverridden" label="${leaderOverriddenColumn}" comparator="${actionBean.leaderOverrideComparator}">${groupData.groupExtension.leaderOverridden ?  leaderOverridden : leaderNotOverridden}</bbNG:listElement>
        <bbNG:listElement name="leader" label="${leaderColumn}" comparator="${actionBean.leaderComparator}">
            <c:choose>
                <c:when test="${groupData.leader != null}">
                    <fmt:message var="leaderName" key="bond.classgroups.tasklist.leadername">
                        <fmt:param value="${groupData.leader.givenName}" />
                        <fmt:param value="${groupData.leader.familyName}" />
                    </fmt:message>
                    ${leaderName}
                </c:when>
                <c:otherwise>${noLeader}</c:otherwise>
            </c:choose>
        </bbNG:listElement>

    </bbNG:pagedList>

</bbNG:learningSystemPage>