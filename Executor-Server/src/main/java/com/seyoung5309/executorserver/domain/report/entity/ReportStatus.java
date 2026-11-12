package com.seyoung5309.executorserver.domain.report.entity;

/** 처리 순서대로 선언한다 (Report.changeStatus가 순서로 진행 방향을 판단) */
public enum ReportStatus {
    RECEIVED, IN_PROGRESS, DONE
}
