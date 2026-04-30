import SummaryConfigForm from '../components/SummaryConfigForm'

export default function SettingsPage() {
  return (
    <div className="max-w-lg mx-auto p-6">
      <h1 className="text-xl font-bold mb-4">サマリー設定</h1>
      <SummaryConfigForm />
    </div>
  )
}
