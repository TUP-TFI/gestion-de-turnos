import { usePing } from './hooks/usePing'

function App() {
  const { data, error, isLoading } = usePing()

  return (
    <div>
      <h1>gestión de turnos</h1>
      <p>
        {isLoading && 'Conectando con el backend...'}
        {error && `Error al conectar con el backend: ${error}`}
        {data && `Backend dice: ${data}`}
      </p>
    </div>
  )
}

export default App
