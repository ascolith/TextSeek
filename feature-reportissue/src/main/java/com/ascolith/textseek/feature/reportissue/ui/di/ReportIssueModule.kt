package com.ascolith.textseek.feature.reportissue.ui.di

import com.ascolith.textseek.feature.reportissue.ui.email.IEmailLauncher
import com.ascolith.textseek.feature.reportissue.ui.email.IssueEmailLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ReportIssueModule {

    @Binds
    fun bindIssueEmailLauncher(issueEmailLauncher: IssueEmailLauncher): IEmailLauncher
}
