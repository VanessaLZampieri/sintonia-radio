const BAR_COUNT = 5

export function Equalizer({ active }: { active: boolean }) {
  return (
    <div className={`equalizer${active ? ' equalizer-active' : ''}`} aria-hidden="true">
      {Array.from({ length: BAR_COUNT }).map((_, index) => (
        <span
          key={index}
          className="equalizer-bar"
          style={{ animationDelay: `${index * 0.14}s` }}
        />
      ))}
    </div>
  )
}
