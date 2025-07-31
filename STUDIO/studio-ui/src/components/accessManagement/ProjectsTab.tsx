import React, { useEffect, useMemo } from 'react'
import { apiCall } from '../../services'
import { Button, Form, Select, Space } from 'antd'
import { MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import { roleOptions } from './utils'
import { Project } from '../../types/projects'
import { SelectOption } from '../form/Select'
import { useTranslation } from 'react-i18next'
import { Repository } from '../../types/repositories'

interface ProjectsTabProps {
    designRepositories: Repository[]
    selectedProjects: string[]
}

export const ProjectsTab: React.FC<ProjectsTabProps> = ({ designRepositories, selectedProjects }) => {
    const { t } = useTranslation()
    const [projects, setProjects] = React.useState<SelectOption[]>([])

    const fetchProjects = async () => {
        const response: Project[] = await apiCall('/projects')
        const projectsWithDesignRepositoriesOptions = response.reduce((acc, project) => {
            let indexOfOption = acc.findIndex(option => option.title === project.repository)
            if (indexOfOption === -1) {
                const designRepository = designRepositories.find(option => option.id === project.repository)
                if (designRepository) {
                    indexOfOption = acc.push({
                        label: designRepository.name,
                        title: designRepository.id,
                        value: null,
                        options: [] as SelectOption[] }) - 1
                }
            }
            if (indexOfOption !== -1) {
                acc[indexOfOption].options.push({ label: project.name, value: project.id })
            }
            return acc
        }, [] as SelectOption[])
        setProjects(projectsWithDesignRepositoriesOptions)
    }
    useEffect(() => {
        fetchProjects()
    }, [])

    const projectsOptions = useMemo(() => {
        return projects.map(project => {
            return {
                label: project.label,
                title: project.title,
                options: project.options.map((option: SelectOption) => ({
                    label: option.label,
                    value: option.value,
                    disabled: selectedProjects.includes(option.value as string)
                })),
            }
        })
    }, [projects, selectedProjects])

    return (
        <Form.List name="projects">
            {(fields, { add, remove }) => (
                <>
                    {fields.map(({ key, name, ...restField }) => (
                        <Space key={key} align="baseline" style={{ display: 'flex', marginBottom: 8 }}>
                            <Form.Item
                                {...restField}
                                name={[name, 'id']}
                                rules={[{ required: true, message: t('common:select_project') }]}
                            >
                                <Select
                                    showSearch
                                    options={projectsOptions}
                                    placeholder={t('common:project')}
                                    style={{ width: 250 }}
                                    filterOption={(input, option) => {
                                        if (!option || !option.label || !(typeof option.label === 'string')) {
                                            return false
                                        }
                                        return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0
                                    }}
                                />
                            </Form.Item>
                            <Form.Item
                                {...restField}
                                name={[name, 'role']}
                                rules={[{ required: true, message: t('common:select_role') }]}
                            >
                                <Select options={roleOptions} placeholder={t('common:role_t')} style={{ width: 250 }} />
                            </Form.Item>
                            <MinusCircleOutlined onClick={() => remove(name)} />
                        </Space>
                    ))}
                    <Form.Item>
                        <Button block icon={<PlusOutlined />} onClick={() => add()} type="dashed">
                            {t('common:add_role')}
                        </Button>
                    </Form.Item>
                </>
            )}
        </Form.List>
    )
}
