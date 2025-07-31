import React, { FC } from 'react'
import { useTranslation } from 'react-i18next'
import { Checkbox, Input, InputNumber, InputPassword } from '../../components'
import { Divider, Form } from 'antd'
import {
    BranchModal,
    DefaultBranchModal, FlatFolderStructureModal,
    LocalPathModal,
    MewBranchRegexErrorModal, NewBranchRegexModal,
    ProtectedBranchesModal,
    RemoteRepositoryModal,
    URLModal,
} from './InfoFieldModals'
import { RepositoryDataType } from './constants'

interface RepositoryGitConfigurationProps {
    repositoryDataType: RepositoryDataType
}

export const RepositoryGitConfiguration: FC<RepositoryGitConfigurationProps> = ({ repositoryDataType }) => {
    const { t } = useTranslation()
    const form = Form.useFormInstance()
    const isRemoteRepository = Form.useWatch(['settings', 'remoteRepository'], form)
    const isFlatFolderStructure = Form.useWatch(['settings', 'flatFolderStructure'], form)

    return (
        <>
            <Checkbox label={t('repository:remote_repository')} name={['settings', 'remoteRepository']} tooltip={{ icon: RemoteRepositoryModal }} />
            {isRemoteRepository && (
                <>
                    <Input
                        label={t('repository:url')}
                        name={['settings', 'uri']}
                        rules={[{ required: true, message: t('common:validation.required') }]}
                        tooltip={{ icon: URLModal }}
                    />
                    <Input label={t('repository:login')} name={['settings', 'login']} />
                    <InputPassword label={t('repository:password')} name={['settings', 'password']} />
                    
                </>
            )}
            <Input
                label={t('repository:local_path')}
                name={['settings', 'localRepositoryPath']}
                rules={[{ required: true, message: t('common:validation.required') }]}
                tooltip={{ icon: LocalPathModal }}
            />
            {isRemoteRepository && (
                <Input label={t('repository:branch')} name={['settings', 'branch']} tooltip={{ icon: BranchModal }} />
            )}
            <Input label={t('repository:protected_branches')} name={['settings', 'protectedBranches']} tooltip={{ icon: ProtectedBranchesModal }} />
            {isRemoteRepository && (
                <>
                    <InputNumber label={t('repository:changes_check_interval')} name={['settings', 'listenerTimerPeriod']} />
                    <InputNumber label={t('repository:connection_timeout')} name={['settings', 'connectionTimeout']} />
                </>
            )}
            {repositoryDataType === RepositoryDataType.DESIGN && (
                <>
                    <Divider orientation="left">{t('repository:new_branch')}</Divider>
                    <Input label={t('repository:default_branch_name')} name={['settings', 'newBranchTemplate']} tooltip={{ icon: DefaultBranchModal }} />
                    <Input label={t('repository:branch_name_pattern')} name={['settings', 'newBranchRegex']} tooltip={{ icon: NewBranchRegexModal }} />
                    <Input label={t('repository:invalid_branch_name_message_hint')} name={['settings', 'newBranchRegexError']} tooltip={{ icon: MewBranchRegexErrorModal }} />
                    <Divider orientation="left">{t('repository:folder_structure')}</Divider>
                    <Checkbox label={t('repository:flat_folder_structure')} name={['settings', 'flatFolderStructure']} tooltip={{ icon: FlatFolderStructureModal }} />
                    {isFlatFolderStructure && (
                        <Input label={t('repository:path')} name={['settings', 'basePath']} />
                    )}
                </>
            )}

        </>
    )
}
